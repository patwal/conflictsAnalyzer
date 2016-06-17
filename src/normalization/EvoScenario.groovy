package normalization

import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTTerminal

import java.io.BufferedReader;
import java.io.InputStreamReader
import java.util.LinkedList;
import java.util.Observable

import main.ExtractorResult;
import main.MergeCommit
import main.SSMergeNode
import merger.FSTGenMerger;
import merger.MergeVisitor
import util.CompareFiles;

class EvoScenario implements Observer{

	String name
	
	String rev_file

	ExtractorResult extractorResult

	boolean isMergeCommit
	
	boolean hasConflictsOnNonJavaFiles

	//node type, number of changes
	Map <String, Integer> changesSummary

	int numberOfChangesInsideMethodsChunks

	int numberOfChangesInsideMethodsLines

	private FSTGenMerger fstGenMerge

	public EvoScenario(MergeCommit mc, ExtractorResult er){
		this.setIsMergeCommit(mc)
		this.extractorResult = er
		this.setName()
		this.setHasConflictsOnNonJavaFiles()
		this.preProcessFiles()
		this.initializeChangesSummary()
	}
	
	public void initializeChangesSummary(){
		this.changesSummary = new HashMap<String, Integer>()
		for(SSMergeNode node in SSMergeNode.values()){
			this.changesSummary.put(node.toString(), 0)
		}
	}
	
	public void setHasConflictsOnNonJavaFiles(){
		if(this.extractorResult.nonJavaFilesWithConflict.size > 0){
			this.hasConflictsOnNonJavaFiles = true
		}else{
			this.hasConflictsOnNonJavaFiles = false
		}
	}

	public void setName(){
		String [] temp = this.extractorResult.revisionFile.split('/')
		String revFile = temp[temp.length -1]
		this.rev_file = revFile.substring(0, revFile.length()-10)
		this.name = temp[temp.length - 2]
	}

	public void preProcessFiles(){
		CompareFiles cp = new CompareFiles(this.extractorResult.revisionFile)
		cp.removeNonJavaFiles()
		cp.removeEqualFiles()
	}


	public void setIsMergeCommit(MergeCommit mc){
		if(!mc.parent2.equals('')){
			this.isMergeCommit = true
		}else{
			this.isMergeCommit = false
		}
	}


	@Override
	public void update(Observable o, Object arg) {
		if(o instanceof MergeVisitor && arg instanceof FSTTerminal){
			if(!arg.getType().contains("-Content") && this.isConflictableNode(arg.getType())){
				if(arg.getBody().contains(FSTGenMerger.MERGE_SEPARATOR)){
					this.computeChanges(arg)
				}else{
					this.updateMetricsWithSingleNodes(arg)
				}
				
			}
			
		}
	}
	
	public boolean isConflictableNode(String nodeType){
		boolean result = false;
		for(SSMergeNode node in SSMergeNode.values()){
			if(nodeType.equals(node.toString())){
				result = true
			}
		}
		
		return result
	}
	
	public void computeChanges(FSTTerminal node){
		String [] tokens = this.getBody(node)
		String nodeType = node.getType()

		this.compareTwoNodes(tokens[0], tokens[1], nodeType)

		if(this.isMergeCommit){
			this.compareTwoNodes(tokens[2], tokens[1], nodeType)
		}
	}

	private void compareTwoNodes(String newFile, String oldFile, String nodeType){
		if(!newFile.equals(oldFile)){
			this.updateChangesSummary(nodeType)

			if(this.isMethodOrConstructor(nodeType)){
				this.computeChangesInsideMC(newFile, oldFile)
			}
		}
	}

	private void updateChangesSummary(String nodeType){	
		try{
			int value = this.changesSummary.get(nodeType)
			int newValue = value + 1
			this.changesSummary.put(nodeType, newValue)
		}catch(Exception e){
			println nodeType
			e.printStackTrace()
			System.exit(0)
		}
			
	}

	public void computeChangesInsideMC(String newFile, String oldFile){
		//if base is empty count as one change
		if(oldFile.equals('')){
			this.numberOfChangesInsideMethodsChunks++
			this.numberOfChangesInsideMethodsLines = this.numberOfChangesInsideMethodsLines +
					newFile.split('\n').length
		}else{
			//run unix diff and compute changes considering chunks and lines
			File oldF = new File('old')
			oldF.write(oldFile)
			File newF = new File('new')
			newF.write(newFile)

			String diffCmd = 'diff -u ' + newF.getAbsolutePath() + ' ' + oldF.getAbsolutePath()
			Runtime run = Runtime.getRuntime()
			Process pr = run.exec(diffCmd)
			String result = pr.text
			this.readUnixDiffResult(result)
			oldF.delete()
			newF.delete()

		}
	}

	private void readUnixDiffResult(String result){
		String [] lines = result.split('\n')
		boolean startedChunk = false
		for(String line in lines){
			if(line.startsWith('+ ') || line.startsWith('- ') || line.startsWith('! ')){
				this.numberOfChangesInsideMethodsLines++
				startedChunk = true
			}else{
				if(startedChunk){
					startedChunk = false
					this.numberOfChangesInsideMethodsChunks++
				}
			}

		}
	}


	public boolean isMethodOrConstructor(String nodeType){
		boolean result = nodeType.equals("MethodDecl") || nodeType.equals("ConstructorDecl");
		return result;
	}

	private String[] getBody(FSTTerminal node){
		String body = node.getBody() + " "
		String[] tokens = body.split(FSTGenMerger.MERGE_SEPARATOR);

		try {
			tokens[0] = tokens[0].replace(FSTGenMerger.SEMANTIC_MERGE_MARKER, "").trim();
			tokens[1] = tokens[1].trim();
			tokens[2] = tokens[2].trim();
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("|"+body+"|");
			e.printStackTrace();
		}

		return tokens
	}

	public void analyseChanges (){
		this.fstGenMerge = new FSTGenMerger()
		fstGenMerge.getMergeVisitor().setIsMergeCommit(this.isMergeCommit)
		fstGenMerge.getMergeVisitor().addObserver(this)
		String[] files = ["--expression", this.extractorResult.revisionFile]
		fstGenMerge.run(files)
		//this.analyseLoneBaseNodes(this.fstGenMerge.baseNodes)
		println 'Finished analyzing scenario ' + this.name
	}
	
	
	public void analyseLoneBaseNodes(LinkedList<FSTNode> bNodes){
		for(FSTNode node in bNodes){
			if(node instanceof FSTTerminal){
				this.updateMetricsWithSingleNodes(node)
			}
		}
	}
	
	public void updateMetricsWithSingleNodes(FSTTerminal node){
		this.updateChangesSummary(node.getType())
		if(this.isMethodOrConstructor(node.getType())){
			this.numberOfChangesInsideMethodsChunks++
			this.numberOfChangesInsideMethodsLines = this.numberOfChangesInsideMethodsLines +
			node.getBody().split('\n').length
		}
		
	}
	
	public void deleteEvoDir(){
		int sub = 0
		if(this.isMergeCommit){
			sub = 26
		}else{
			sub = 25
		}
		String msPath = this.extractorResult.revisionFile.substring(0, (this.extractorResult.revisionFile.length()-sub))
		File dir = new File(msPath)
		boolean deleted = dir.deleteDir()
		if(deleted){
			println 'Evo scenario ' + msPath + ' deleted!'
		}else{

			println 'Evo scenario ' + msPath + ' not deleted!'
		}
	}

	public String toString(){
		String result = this.name + ', '
		
		//print changes considering nodes
		for(SSMergeNode node in SSMergeNode.values()){
				result = result + this.changesSummary.get(node.toString()) + ', '
		}
		
		//print other metrics
		
		result = result + this.numberOfChangesInsideMethodsChunks + ', ' +
		this.numberOfChangesInsideMethodsLines
		
		return result
	}
	
	

	public static void main (String[] args){
		MergeCommit mc = new MergeCommit()
		mc.sha = 'a2ac73ff7af23fb23c86b0cd18e1337d2feb1122'
		mc.parent1 = '935c895593495ede2e0298f8e59a6cfad2a05502'
		mc.parent2 = ''
		/*mc.sha = '3ba28a912a763ff973e3d5ab63ac35a64462c117'
		mc.parent1 = '448259185594ed4f0b9ea2c6be9197ca3f5573db'
		mc.parent2 = 'c0c6d0e7b0f175b800925472be4c550e5a39567d'*/
		ExtractorResult er = new ExtractorResult()
		er.revisionFile = '/Users/paolaaccioly/Documents/Doutorado/workspace_fse/downloads/TGM/revisions/rev_a2ac7/rev_935c8-none.revisions'
		//er.revisionFile = '/Users/paolaaccioly/Documents/Doutorado/workspace_fse/downloads/TGM/revisions/rev_3ba28/rev_44825-c0c6d.revisions'
		EvoScenario evo = new EvoScenario(mc, er)
		evo.analyseChanges()
		NormalizedConflictPrinter.printEvoScenarioReport(evo, 'TGM')
		if(evo.hasConflictsOnNonJavaFiles){
			NormalizedConflictPrinter.printMergeScenariosWithConflictsOnNonJavaFiles(evo, 'TGM')
		}
		//scenario.deleteEvoDir()
		/*String newFile = new File('/Users/paolaaccioly/Desktop/left.txt').getText()
		 String oldFile = new File('/Users/paolaaccioly/Desktop/base.txt').getText()
		 evo.compareTwoNodes(newFile, oldFile, 'MethodDecl')
		 println() 'hello'*/
		
		/*String diffCmd = 'diff -u /Users/paolaaccioly/Desktop/left.txt /Users/paolaaccioly/Desktop/base.txt'
		 Runtime run = Runtime.getRuntime()
		 Process pr = run.exec(diffCmd)
		 String result = pr.getText()
		 println result*/
		
	}
}
