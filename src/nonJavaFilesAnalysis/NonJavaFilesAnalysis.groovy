package nonJavaFilesAnalysis

import main.Extractor
import main.ExtractorResult;
import main.GremlinProject
import main.MergeCommit
import util.CSVAnalyzer;;

class NonJavaFilesAnalysis {

	String resultData
	String downloads
	String projectsList
	

	public NonJavaFilesAnalysis(String projectslist, String resultData, String downloads){
		this.projectsList = projectslist
		this.resultData = resultData
		this.downloads = downloads
		
	}

	public void analyseNonJavaFiles(){
		File list = new File(this.projectsList)
		list.eachLine {
			String projectRepo = it.trim()
			String projectName = this.getProjectname(projectRepo)
			this.analyseProject(projectName, projectRepo)
		}
	}

	public String getProjectname(String repo){
		String[] projectData = repo.split('/')
		String result = projectData[1].trim()

		return result
	}

	public void analyseProject(String name, String repo){
		//load projects summary
		ProjectSummary summary = this.loadProjectSummary(name)
		
		//read merge commit file
		ArrayList<MergeCommit> mergeCommits = this.readMergeCommitsFile(name)
		
		//change start when project analysis crashes
		int start = 4903
		int end = mergeCommits.size()
		
		//initialize extractor
		Extractor extractor = this.createExtractor(name, repo)
		
		//analyze merge commits
		while(start<end){
			
			int counter = start +1
			println 'Starting to analyse merge commit [' + counter + '] from [' + end + '] from project ' + name
			MergeCommit mc = mergeCommits.getAt(start)
			
			ExtractorResult er = extractor.getConflictingfiles(mc.parent1, mc.parent2)
			
			if(!er.revisionFile.equals('') && er.nonJavaFilesWithConflict.size>0){
				summary.mergeCommitsConflictsNonJavaFiles.add(er.revisionFile)
				this.printMergeCommit(name, er.revisionFile)
				this.printProjectSummaryIteration(summary)
				
			}
			start++
		}
		
		this.printProjectSummaryFinal(summary)
		
	}

	public ArrayList<MergeCommit> readMergeCommitsFile(String name){
		ArrayList<MergeCommit> result = new ArrayList<MergeCommit>()
		String filePath = this.resultData + File.separator + name + File.separator + 'mergeCommits.csv'
		File mergeCommitsFile = new File(filePath)
		if(mergeCommitsFile.exists()){
			mergeCommitsFile.eachLine {
				if(!it.startsWith('Merge')){
					MergeCommit mc = this.readMergeCommit(it.trim())
					result.add(mc)
				}
			}
		}

		return result
	}

	public MergeCommit readMergeCommit(String mc){
		MergeCommit result = new MergeCommit()
		String [] tokens = mc.split(',')
		result.sha = tokens[0].trim()
		result.parent1 = tokens[1].trim()
		result.parent2 = tokens[2].trim()
		return result
	}
	
	private Extractor createExtractor(String projectName, String projectRepo){
		GremlinProject gProject = new GremlinProject(projectName, projectRepo)
		Extractor extractor = new Extractor(gProject, this.downloads)

		return extractor
	}
	
	private printMergeCommit(String projectName, String rev_name){
		String dirPath = 'ResultData' + File.separator + projectName
		File dir = new File(dirPath)
		if(!dir.exists()){
			dir.mkdirs()
		}
		File out = new File(dirPath + File.separator + 'mergeWithNonJavaFilesConflicting.csv') 
		
		out.append(rev_name + '\n')
		
	}
	
	private printProjectSummaryIteration(ProjectSummary summary){
		String dirPath = 'ResultData' + File.separator + summary.name
		File dir = new File(dirPath)
		if(!dir.exists()){
			dir.mkdirs()
		}
		File out = new File(dirPath + File.separator + 'ConflictingScenarios.csv')
		out.delete()
		out.append('ProjectName, TotalMC, MCJava, MCJavaWFP, MCNonJava, MCNonJavaMinusMCJava, MCNonJavaMinusMCJavaWFP\n')
		out.append(summary.toString() + '\n')
	}
	
	private printProjectSummaryFinal(ProjectSummary summary){
		String dirPath = 'ResultData'
		File dir = new File(dirPath)
		if(!dir.exists()){
			dir.mkdirs()
		}
		File out = new File(dirPath + File.separator + 'ConflictingScenarios.csv')
		if(!out.exists()){
			out.append('ProjectName, TotalMC, MCJava, MCJavaWFP, MCNonJava, MCNonJavaMinusMCJava, MCNonJavaMinusMCJavaWFP\n')
		}

		out.append(summary.toString() + '\n')
	}
	
	private ProjectSummary loadProjectSummary(String name){
		//initializes summary
		ProjectSummary result = new ProjectSummary(name)
		
		//read project's merge scenario report
		String fileName = this.resultData + File.separator + name + File.separator + 'MergeScenariosReport.csv'
		File file = new File(fileName)
		if(file.exists()){
			file.eachLine {
				if(!it.startsWith('Merge')){
					
					String[] tokens= it.split(',')
					String rev_name = tokens[0]
					
					//adds merge to total merge set
					result.totalMergeCommits.add(rev_name)
					
					//adds merge to merge with conflicts on java files set
					int hasConflictsJavaFiles = Integer.parseInt(tokens[6].trim())
					if(hasConflictsJavaFiles!=0){
						result.mergeCommitsConflictsJavaFiles.add(rev_name)
					}
					
					//adds merge on merge with 'real' conflicts on java files set
					if(CSVAnalyzer.hasRealConflicts(it)){
						result.mergeCommitsConflictsJavaFilesWFP.add(rev_name)
					}
					
					
				}
			}
		}
		return result
	}
	
	public static void main(String[] args){
		NonJavaFilesAnalysis n = new NonJavaFilesAnalysis('projectsList', '/Users/paolaaccioly/Documents/testeConflictsAnalyzer/conflictsAnalyzer/ResultData',
				'/Users/paolaaccioly/Documents/testeConflictsAnalyzer/downloads')
		n.analyseNonJavaFiles()

	}

}
