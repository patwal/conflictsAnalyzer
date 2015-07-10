package main

class Project {

	private ArrayList<MergeScenario> mergeScenarios

	private String name

	private int analyzedMergeScenarios

	private int conflictingMergeScenarios

	private double conflictRate

	private Hashtable<String, Integer> projectSummary

	public Project(String projectName, String mergeScenariosPath){

		this.name = projectName
		initializeProjectSummary()
		createMergeScenarios(mergeScenariosPath)
		initializeProjectMetrics()
		this.createProjectDir()
	}

	/*The following constructor is used to initialize projects
	 * that were already analyzed
	 */
	public Project(String projectName, int totalScenarios, int conflictingscenarios,
	Hashtable<String, Integer> projectSummary){

		this.name = projectName
		this.analyzedMergeScenarios = totalScenarios
		this.conflictingMergeScenarios = conflictingscenarios
		this.computeConflictingRate()
		this.projectSummary = projectSummary
	}
	
	/*This constructor is used by the CsvAnalyzer class*/
	public Project(String projectName){
		this.name = projectName
	}
	
	private void createProjectDir(){
		FileWithConflicts projectDir = new FileWithConflicts( "ResultData" + FileWithConflicts.separator + this.name)
		if(!projectDir.exists()){
			projectDir.mkdir()
		}

	}

	private initializeProjectMetrics() {
		this.analyzedMergeScenarios = 0
		this.conflictingMergeScenarios = 0
		this.conflictRate = 0.0
	}

	public void createMergeScenarios(String mergeScenariosPath){
		this.mergeScenarios = new ArrayList<MergeScenario>()
		def mergeScenarioFile = new FileWithConflicts(mergeScenariosPath)
		mergeScenarioFile.eachLine {
			MergeScenario ms = new MergeScenario(it)
			this.mergeScenarios.add(ms)
		}

	}

	public void setMergeScenarios(ArrayList<MergeScenario> ms){

		this.mergeScenarios = ms

	}

	public ArrayList<MergeScenario> getMergeScenarios(){

		return this.mergeScenarios

	}

	public void setName(String name){

		this.name = name

	}

	public String getName(){

		return this.name

	}

	public double getConflictRate(){
		return this.conflictRate
	}

	public Hashtable<String, Integer> getProjectSummary(){
		return this.projectSummary
	}

	public void analyzeConflicts(){

		for(MergeScenario ms : this.mergeScenarios){
			ms.analyzeConflicts()
			updateAndPrintSummary(ms)
			ms.deleteMSDir()

		}
	}

	private printResults(MergeScenario ms) {
		ConflictPrinter.printMergeScenarioReport(ms, this.name)
		ConflictPrinter.printProjectReport(this)
	}

	private void updateAndPrintSummary(MergeScenario ms){
		updateConflictingRate(ms)
		if(ms.hasConflicts){
			updateProjectSummary(ms)
		}
		printResults(ms)
	}

	private updateConflictingRate(MergeScenario ms) {
		this.analyzedMergeScenarios++
		if(ms.hasConflicts){
			this.conflictingMergeScenarios++
		}
		this.computeConflictingRate()
	}

	private void computeConflictingRate(){

		double cr = (this.conflictingMergeScenarios/
				this.analyzedMergeScenarios) * 100
		this.conflictRate = cr.round(2)

	}

	private void initializeProjectSummary(){

		this.projectSummary = new Hashtable<String, Integer>()

		for(SSMergeConflicts c : SSMergeConflicts.values()){

			String type = c.toString()
			projectSummary.put(type, 0)
		}


	}

	private void updateProjectSummary(MergeScenario ms){
		Set<String> keys = this.projectSummary.keySet()

		for(String key: keys){
			int mergeQuantity = ms.mergeScenarioSummary.get(key).value
			int projectQuantity = this.projectSummary.get(key).value
			projectQuantity = projectQuantity + mergeQuantity
			this.projectSummary.put(key, projectQuantity)
		}
	}

}
