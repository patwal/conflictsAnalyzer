package nonJavaFilesAnalysis

import com.sun.org.apache.bcel.internal.generic.RETURN;

class ProjectSummary {

	String name
	List totalMergeCommits
	List mergeCommitsConflictsJavaFiles
	List mergeCommitsConflictsJavaFilesWFP
	List mergeCommitsConflictsNonJavaFiles

	public ProjectSummary(String name){
		this.name = name
		totalMergeCommits = new ArrayList<String>()
		mergeCommitsConflictsJavaFiles = new ArrayList<String>()
		mergeCommitsConflictsJavaFilesWFP = new ArrayList<String>()
		mergeCommitsConflictsNonJavaFiles = new ArrayList<String>()
	}
	
	public void teste(){
		ArrayList<String> resultado = this.removeOneListFromTheOther(this.mergeCommitsConflictsNonJavaFiles, this.totalMergeCommits)
		println 'hello'
	}
	
	public String toString(){
		String result = ''
		ArrayList<String> NonJavaMinusJava = this.removeOneListFromTheOther(this.mergeCommitsConflictsNonJavaFiles, this.mergeCommitsConflictsJavaFiles)
		ArrayList<String> NonJavaMinusJavaWFP = this.removeOneListFromTheOther(this.mergeCommitsConflictsNonJavaFiles, this.mergeCommitsConflictsJavaFilesWFP)
		

		result = this.name + ', ' + this.totalMergeCommits.size() + ', ' +
				this.mergeCommitsConflictsJavaFiles.size() + ', ' + this.mergeCommitsConflictsJavaFilesWFP.size() +
				', ' + this.mergeCommitsConflictsNonJavaFiles.size() + ', '+ NonJavaMinusJava.size() + ', ' +
				NonJavaMinusJavaWFP.size()

		return result
	}

	public ArrayList<String> removeOneListFromTheOther(ArrayList<String> first, ArrayList<String> second){
		ArrayList<String> result = new ArrayList<String>(first)
		for(String s in second){
			if(first.contains(s)){
				result.remove(s)
			}
		}
		return result
	}


}
