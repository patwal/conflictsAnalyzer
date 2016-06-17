package util

import main.SSMergeConflicts

class CSVAnalyzer {

	public static void verifyDiffsOnSameSignatureMC(){
		File file = new File('/Users/paolaaccioly/Documents/testeConflictsAnalyzer/conflictsAnalyzer/projectsPatternData.csv')
		file.eachLine {
			String[] data = it.split(",")
			String projectName = data[0]
			if(!projectName.equals("Project")){
				int sameSignatureCM = Integer.parseInt(data[27])
				int smallMethod = Integer.parseInt(data[36])
				int renamedMethod = Integer.parseInt(data[37])
				int copiedMethod = Integer.parseInt(data[38])
				int copiedFile = Integer.parseInt(data[39])
				int noPattern = Integer.parseInt(data[40])

				int sumCauses = smallMethod + renamedMethod + copiedMethod + copiedFile + noPattern

				int diff = sameSignatureCM - sumCauses

				if(diff != 0){
					println projectName + ' ' + diff
				}
			}
		}
	}

	public static void writeRealConflictsCSV(){
		String filepath = System.getProperty("user.dir") + File.separator + 'projectsPatternData.csv'
		File file = new File(filepath)
		File out = new File('realConflictRate.csv')
		out.delete()
		out = new File('realConflictRate.csv')
		String line = 'Project,Merge Scenarios,Conflicting Scenarios\n'
		out.append(line)

		file.eachLine {
			String[] data = it.split(",")
			String projectName = data[0]
			String analyzedMergeScenarios = data[1]
			if(!projectName.equals("Project")){
				int i = countMergeScenarioWithRealConflicts(projectName)
				line = projectName + ',' + analyzedMergeScenarios + ',' + i + '\n'
				//println line
				out.append(line)
			}
		}
	}

	public static int countMergeScenarioWithRealConflicts(String projectName){

		int result = 0
		/*String mergeScenarioFile = 'ResultData' + File.separator + projectName + File.separator +
		 'MergeScenariosReport.csv'*/
		try {
			String mergeScenarioFile = "ResultData/" + projectName + "/MergeScenariosReport.csv";
			File file = new File(mergeScenarioFile);
			if(file.exists()) {
				String msFile = file.text
				String [] lines = msFile.split('\n')
				for(int i = 1; i< lines.length;  i++){
					if(hasRealConflicts(lines[i])){
						result++
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace()
		}
		return result
	}

	public static boolean hasRealConflicts(String line){
		boolean hasRealConflicts = false
		String[] data = line.split(', ')
		int i = 7
		for(SSMergeConflicts c : SSMergeConflicts.values()){
			if(!c.toString().equals(SSMergeConflicts.NOPATTERN.toString())){
				int total = Integer.parseInt(data[i])
				i++
				int ds = Integer.parseInt(data[i])
				i++
				int cl = Integer.parseInt(data[i])
				i++
				int ifp = Integer.parseInt(data[i])
				int realConflicts = total - ds - cl + ifp
				if(realConflicts > 0){
					hasRealConflicts = true
				}
				i++
			}
		}

		return hasRealConflicts
	}



	public static void writeFileMetricsCSV(){
		File file = new File('/Users/paolaaccioly/Documents/testeConflictsAnalyzer/conflictsAnalyzer/projectsPatternData.csv')
		File out = new File('filesMetrics.csv')
		out.delete()
		out = new File('filesMetrics.csv')
		String line = 'Project,Total_files,Files_merged,Files_with_conflicts\n'
		out.append(line)

		file.eachLine {
			String[] data = it.split(",")
			String projectName = data[0]
			if(!projectName.equals("Project")){
				line = this.computeFileMetrics(projectName)
				out.append(line + '\n')
				println line
			}
		}
	}

	public static String computeFileMetrics(String projectName){
		File out = new File('moreThan10.csv')

		if(!out.exists()){
			out.append('Project,MergedFiles,FilesWithConflict\n')
		}

		String result = ''
		int totalFiles, mergedFiles, filesWithConflicts
		int[] wasMoreThan90 = [0, 0]
		String mergeScenarioFile = 'ResultData' + File.separator + projectName + File.separator +
				'MergeScenariosReport.csv'
		String msFile = new File(mergeScenarioFile).text
		String [] lines = msFile.split('\n')
		for(int i = 1; i< lines.length;  i++){
			String[] data = lines[i].split(', ')
			totalFiles = totalFiles + Integer.parseInt(data[1]) + Integer.parseInt(data[4])
			mergedFiles = mergedFiles + Integer.parseInt(data[5])
			filesWithConflicts = filesWithConflicts + Integer.parseInt(data[6])
			int[] r = this.checkPercentagePerMC(totalFiles, mergedFiles,filesWithConflicts)
			wasMoreThan90[0] =  wasMoreThan90[0] + r[0]
			wasMoreThan90[1] =  wasMoreThan90[1] + r[1]
		}
		String a = projectName + ',' + wasMoreThan90[0] +',' + wasMoreThan90[1]
		out.append(a + '\n')
		result = projectName + ',' +totalFiles + ',' + mergedFiles + ',' + filesWithConflicts
		return result
	}

	public static int[] checkPercentagePerMC(int totalFiles, int mergedFiles,int filesWithConflicts){
		int[] result = [0, 0]
		int percentageMF = 0
		int percentageFWC =0
		if(totalFiles != 0){
			percentageMF = (mergedFiles/totalFiles)*100
			percentageFWC = (filesWithConflicts/totalFiles)*100
		}

		if(percentageMF >=10){
			result[0] = 1
		}
		if(percentageFWC>=10){
			result[1] = 1
		}

		return result
	}
	public static void main(String[] args){
		CSVAnalyzer.writeRealConflictsCSV()
		//CSVAnalyzer.writeFileMetricsCSV()
	}
}
