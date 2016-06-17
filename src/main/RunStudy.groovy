package main
import java.util.Hashtable

import getConflictCommits.PaolaMergeConflicter;
import util.CSVAnalyzer
import util.CurrentProject;;

/*this class is supposed to integrate all the 3 steps involved to run the study
 * gitminer/gremlinQuery/ConflictsAnalyzer
 */

class RunStudy {


	private String gitminerConfigProps = 'gitminerConfiguration.properties'
	private String projectName
	private String projectRepo
	private String gitminerLocation
	private String downloadPath
	private Hashtable<String, Conflict> projectsSummary

	public RunStudy(){
		
		ConflictPrinter.setconflictReportHeader()
	}

	public void run(String[] args){
		def projectsList = new File(args[0])
		this.createResultDir()
		updateGitMinerConfig(args[1])
		
		//for each project
		projectsList.eachLine {
			//run gitminer
			setProjectNameAndRepo(it)
			//attention, if you have already download gitminer base you can comment the line below and use the second line below
			String graphBase = runGitMiner()
			//String graphBase = this.gitminerLocation + File.separator + this.projectName + 'graph.db'
			
			//create project and extractor
			Extractor extractor = this.createExtractor(this.projectName, graphBase)
			Project project = new Project(this.projectName)
			
			//get list of merge commits
			//ArrayList<MergeCommit> listMergeCommits = runGremlinQuery(graphBase)
			
			/******************** HÄR **********************/
			String repoPath = this.downloadPath + "/temp/" + this.projectName + "/git"
			PaolaMergeConflicter pmc = new PaolaMergeConflicter()
			ArrayList<MergeCommit> listMergeCommits = pmc.doMerge(repoPath)
			/***********************************************/
			
			
			//for each merge scenario, clone and run SSMerge on it
			analyseMergeScenarios(listMergeCommits, extractor, project)
			
			//print project report and call R script
			ConflictPrinter.printProjectData(project)
			this.callRScript()
		}

	}

	private void analyseMergeScenarios(ArrayList listMergeCommits, Extractor extractor, 
		Project project) {
		
		//if project execution breaks, update current with next merge scenario number
		int current = 0;
		int end = listMergeCommits.size()
		
		while(current < end){
			int index = current + 1;
			println 'Analyzing merge scenario [' + index + '] from a total of [' + end +
					'] merge scenarios\n'

			MergeCommit mc = listMergeCommits.get(current)
			ExtractorResult er = extractor.extractCommit(mc)
			String revisionFile = er.getRevisionFile()
			if(!revisionFile.equals("")){
				runConflictsAnalyzer(project, er)
			}
			current++
		}
	
	}
	
	private Extractor createExtractor(String projectName, String graphBase){
		GremlinProject gProject = new GremlinProject(this.projectName,
			this.projectRepo, graphBase)
	   Extractor extractor = new Extractor(gProject, this.downloadPath)
	   
	   return extractor
	}
	
	public Hashtable<String, Conflict> getProjectsSummary(){
		return this.projectsSummary
	}
	
	public void updateGitMinerConfig(String configFile){
		Properties gitminerProps =  new Properties()
		File gitminerPropsFile = new File(this.gitminerConfigProps)
		gitminerProps.load(gitminerPropsFile.newDataInputStream())


		Properties configProps = new Properties()
		File propsFile = new File(configFile)
		configProps.load(propsFile.newDataInputStream())

		this.gitminerLocation = configProps.getProperty('gitminer.path')
		this.downloadPath = configProps.getProperty('downloads.path')
		String graphDb = this.gitminerLocation + File.separator + 'graph.db'
		String repo_Loader = this.gitminerLocation + File.separator + 'repo_loader'

		gitminerProps.setProperty('net.wagstrom.research.github.dburl', graphDb)
		gitminerProps.setProperty('edu.unl.cse.git.localStore', repo_Loader)
		gitminerProps.setProperty('edu.unl.cse.git.repositories', graphDb)
		gitminerProps.setProperty('edu.unl.cse.git.dburl', graphDb)

		gitminerProps.setProperty('net.wagstrom.research.github.login', configProps.getProperty('github.login'))
		gitminerProps.setProperty('net.wagstrom.research.github.password', configProps.getProperty('github.password'))
		gitminerProps.setProperty('net.wagstrom.research.github.email', configProps.getProperty('github.email'))
		gitminerProps.setProperty('net.wagstrom.research.github.token', configProps.getProperty('github.token'))

		gitminerProps.store(gitminerPropsFile.newWriter(), null)

	}

	public void setProjectNameAndRepo(String project){
		String[] projectData = project.split('/')
		this.projectName = projectData[1].trim()
		this.projectRepo = project
		CurrentProject.currentRepo = project
		println "Starting project " + this.projectName
	}


	public String runGitMiner(){
		updateProjectRepo()
		println "Running gitminer"
		runGitminerCommand('./gitminer.sh')
		runGitminerCommand('./repository_loader.sh')
		String graphBase = renameGraph()
		println "Finished running gitminer"
		return graphBase
	}


	private String renameGraph(){

		String oldFile = this.gitminerLocation + File.separator + 'graph.db'
		String newFile = this.gitminerLocation + File.separator + this.projectName + 'graph.db'
		new File(oldFile).renameTo(new File(newFile))

		return newFile
	}

	public void runGitminerCommand(String command){
		String propsFile = new File("").getAbsolutePath() + File.separator + this.gitminerConfigProps
		ProcessBuilder pb = new ProcessBuilder(command, "-c", propsFile)
		pb.directory(new File(this.gitminerLocation))
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
		// Start the process.
		try {
			Process p = pb.start()
			p.waitFor()
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private void updateProjectRepo(){
		Properties gitminerProps = new Properties()
		File gitminerPropsFile = new File(this.gitminerConfigProps)
		gitminerProps.load(gitminerPropsFile.newDataInputStream())
		gitminerProps.setProperty('net.wagstrom.research.github.projects', this.projectRepo)
		gitminerProps.setProperty('edu.unl.cse.git.repositories', this.projectRepo)
		gitminerProps.store(gitminerPropsFile.newWriter(), null)
	}

	public ArrayList<MergeCommit> runGremlinQuery(String graphBase){
		println "starting to query the gremlin database and download merge revision"
		GremlinQueryApp gq = new GremlinQueryApp()
		ArrayList<MergeCommit> listMergeCommits = gq.run(projectName, projectRepo, graphBase)
		return listMergeCommits
	}

	public void runConflictsAnalyzer(Project project, ExtractorResult extractResult){
		
		println "starting to run the conflicts analyzer on revision " + extractResult.revisionFile
		project.analyzeConflicts(extractResult)
		
	}
	
	public void callRScript(){
		CSVAnalyzer.writeRealConflictsCSV()
		String propsFile = "resultsScript.r"
		ProcessBuilder pb = new ProcessBuilder("Rscript", propsFile)
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
		// Start the process.
		try {
			Process p = pb.start()
			p.waitFor()
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void createResultDir(){
		File resultDir = new File ('ResultData')
		if(!resultDir.exists()){
			resultDir.mkdirs()
		}
	}
	
	public static void main (String[] args){
		RunStudy study = new RunStudy()
		String[] files= ['projectsList', 'configuration.properties']
		study.run(files)
		
		File file1 = new File("realConflictRate.csv")
		File file2 = new File("projectsPatternData.csv")
		file1.delete()
		file2.delete()
		
	}

}
