package main;


import java.io.File;
import java.util.ArrayList;

import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTTerminal;
import merger.FSTGenMerger;


enum SSMergeConflicts {

	ModifierList, DefaultValueAnnotation, ImplementList, EditSameMC, AddSameFd, 
	EditSameFd, SameSignatureCM, ExtendsList, NOPATTERN

}

public  class Conflict {

	public static final String SSMERGE_SEPARATOR = "##FSTMerge##";

	public static final String DIFF3MERGE_SEPARATOR = "|||||||";

	private String type;

	private String body;

	private String filePath;

	private String nodeType;

	private int differentSpacing;

	private int consecutiveLines;

	private int numberOfConflicts;
	
	private int falsePositivesIntersection;


	public Conflict(FSTTerminal node, String path){
		this.body = node.getBody();
		this.nodeType = node.getType();
		this.matchPattern();
		this.retrieveFilePath(node, path);
		this.setFalsePositives();
	}
	
	
	
	public int getFalsePositivesIntersection() {
		return falsePositivesIntersection;
	}



	public void setFalsePositivesIntersection(int falsePositivesIntersection) {
		this.falsePositivesIntersection = falsePositivesIntersection;
	}



	public void setFalsePositives(){
		if(this.type.equals(SSMergeConflicts.EditSameMC.toString()) || 
				this.type.equals(SSMergeConflicts.EditSameFd.toString())){
			this.countConflictsInsideMethods();
			this.checkFalsePositives();

		}else{
			this.consecutiveLines = 0;
			this.differentSpacing = 0;
			this.numberOfConflicts = 1;
		}
	}

	public void checkFalsePositives(){
		if(this.isMethodOrConstructor()){
			ArrayList<String> conflicts = splitConflictsInsideMethods();
			for(String s : conflicts){
				this.auxCheckFalsePositives(s);
			}
		} else{
			this.auxCheckFalsePositives(this.body);
		}
	}

	private void auxCheckFalsePositives(String s) {
		
		boolean diffSpacing = this.checkDifferentSpacing(s);
		boolean consecLines = this.checkConsecutiveLines(s);
		if(diffSpacing && consecLines){
			this.falsePositivesIntersection++;
		}
	}

	private ArrayList<String> splitConflictsInsideMethods(){
		String [] temp = this.body.split("<<<<<<<");
		ArrayList<String> conflicts = new ArrayList<String>();
		for(int i = 1; i < temp.length; i++){
			String temp2 = temp[i].split(">>>>>>>")[0];
			conflicts.add(temp2);
		}

		return conflicts;
	}

	public boolean checkDifferentSpacing(String s){
		boolean falsePositive = false;
		String [] splitConflictBody = this.splitConflictBody(s);
		String[] threeWay = this.removeInvisibleChars(splitConflictBody);
		if(threeWay[0].equals(threeWay[1]) || threeWay[2].equals(threeWay[1])){
			this.differentSpacing++;
			falsePositive = true;
		}
		return falsePositive;
	}

	public String[] removeInvisibleChars(String[] input){
		input[0] = input[0].replaceAll("\\s+","");
		input[1] = input[1].replaceAll("\\s+","");
		input[2] = input[2].replaceAll("\\s+","");
		return input;
	}

	public boolean checkConsecutiveLines(String s){
		boolean falsePositive = false;
		String [] splitConflictBody = this.splitConflictBody(s);
		String [] leftLines = splitConflictBody[0].split("\n");
		String [] baseLines = splitConflictBody[1].split("\n");
		String [] rightLines = splitConflictBody[2].split("\n");
		
		if(baseLines.length != 0){
			String fixedElement =  baseLines[0];
			boolean foundOnLeft = this.searchFixedElement(fixedElement, leftLines);
			if(foundOnLeft){
				falsePositive = true;
				this.consecutiveLines++;
			}else{
				boolean foundOnRight = this.searchFixedElement(fixedElement, rightLines);
				if(foundOnRight){
					falsePositive = true;
					this.consecutiveLines++;
				}
			}

		}
		return falsePositive;
	}
	
	private boolean searchFixedElement(String fixedElement, String[] variant){
		boolean foundFixedElement = false;
		int i = 0;
		while(!foundFixedElement && i < variant.length){
			if(variant[i].equals(fixedElement)){
				foundFixedElement = true;
			}
			i++;
		}
		return foundFixedElement;
	}
	
	public String [] splitConflictBody(String s){
		String [] splitBody = {"", "", ""};
		if(this.isMethodOrConstructor()){
			if(s.contains("\\|\\|\\|\\|\\|\\|\\|")){
				String[] temp = s.split("\\|\\|\\|\\|\\|\\|\\|");

				String[] temp2 = temp[0].split("\n");
				splitBody[0] = extractLines(temp2);

				String [] baseRight = temp[1].split("=======");	
				temp2 = baseRight[0].split("\n");
				splitBody[1] = extractLines(temp2);
				temp2 = baseRight[1].split("\n");
				splitBody[2] = extractLines(temp2);
			}else{
				splitBody[1] = "";
				splitBody[0] = extractLines(s.split("=======")[0].split("\n"));
				splitBody[2] = extractLines(s.split("=======")[1].split("\n"));
			}

		}else{
			String[] tokens = body.split(FSTGenMerger.MERGE_SEPARATOR);
			splitBody[0] = tokens[0].replace(FSTGenMerger.SEMANTIC_MERGE_MARKER, "").trim();
			splitBody[1] = tokens[1].trim();
			splitBody[2] = tokens[2].trim();

		}

		return splitBody;
	}

	private String extractLines(String[] conflict) {
		String lines = "";
		if(conflict.length > 1){
			for(int i = 1; i < conflict.length; i++){
				if(i != conflict.length-1){
					lines = lines + conflict[i] + "\n";
				}else{
					lines = lines + conflict[i];
				}
				
			}
			
		}
		return lines;
	}


	public void matchPattern(){

		String nodeType = this.nodeType;
		String conflictType = "";

		if(nodeType.equals("Modifiers")){

			conflictType = SSMergeConflicts.ModifierList.toString();

		}else if(nodeType.equals("AnnotationMethodDecl")){

			conflictType = SSMergeConflicts.DefaultValueAnnotation.toString();

		}else if(nodeType.equals("ImplementsList")){

			conflictType = SSMergeConflicts.ImplementList.toString();

		}else if(nodeType.equals("FieldDecl") ){

			conflictType = this.setFieldDeclPattern();

		}else if(isMethodOrConstructor()){

			conflictType = this.setMethodPattern();

		}else if(nodeType.equals("ExtendsList")){

			conflictType = SSMergeConflicts.ExtendsList.toString();
		}

		if (conflictType.equals("")){
			conflictType = SSMergeConflicts.NOPATTERN.toString();
		}

		this.setType(conflictType);

	}

	public boolean isMethodOrConstructor(){
		boolean result = nodeType.equals("MethodDecl") || nodeType.equals("ConstructorDecl");	
		return result;
	}

	public String setFieldDeclPattern(){

		String type = "";
		String [] fd = this.body.split(Conflict.SSMERGE_SEPARATOR);

		if(fd[1].equals(" ")){

			type = SSMergeConflicts.AddSameFd.toString();

		}else{
			type = SSMergeConflicts.EditSameFd.toString();
		}

		return type;

	}

	public String setMethodPattern(){

		String type = "";

		if(isInsideMethod()){
			type = SSMergeConflicts.EditSameMC.toString();
		}else{
			type = matchConflictOutsideMethod();
		}

		return type;

	}

	private String matchConflictOutsideMethod() {
		String type;
		String [] p1 = this.body.split("\\|\\|\\|\\|\\|\\|\\|");
		String [] p2 = p1[1].split("=======");
		String [] p3 = p2[0].split("\n");

		if(p3.length != 1){

			type = SSMergeConflicts.EditSameMC.toString();
		}else{

			type = SSMergeConflicts.SameSignatureCM.toString();

		}
		return type;
	}

	public boolean isInsideMethod(){
		boolean isInsideMethod = false;

		String [] p1 = this.body.split("<<<<<<<");
		String [] p2 = this.body.split(">>>>>>>");
		String [] p3 = p2[p2.length -1].split("\n");
		if(!p1[0].equals("") && p3.length != 1){
			isInsideMethod = true;
		}

		return isInsideMethod;
	}

	public void retrieveFilePath(FSTNode node, String path){

		int endIndex = path.length() - 10;
		String systemDir = path.substring(0, endIndex);

		this.filePath = systemDir + this.retrieveFolderPath(node);
	}

	public String retrieveFolderPath(FSTNode node){
		String filePath = "";
		String nodetype = node.getType();

		if(nodetype.equals("Java-File") || nodetype.equals("Folder")){

			filePath = this.retrieveFolderPath(node.getParent()) + File.separator + node.getName();

			return filePath;

		}else if(nodetype.equals("Feature")){

			return "";

		}else{

			return this.retrieveFolderPath(node.getParent());
		}




	}

	public void countConflictsInsideMethods(){
		String[] p = this.body.split("<<<<<<<");
		if(p.length>1){
			this.numberOfConflicts = p.length - 1;
		}else{
			this.numberOfConflicts = 1;
		}


	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public int getNumberOfConflicts() {
		return numberOfConflicts;
	}

	public void setNumberOfConflicts(int numberOfConflicts) {
		this.numberOfConflicts = numberOfConflicts;
	}

	public int getDifferentSpacing() {
		return differentSpacing;
	}

	public void setDifferentSpacing(int differentSpacing) {
		this.differentSpacing = differentSpacing;
	}

	public int getConsecutiveLines() {
		return consecutiveLines;
	}

	public void setConsecutiveLines(int consecutiveLines) {
		this.consecutiveLines = consecutiveLines;
	}

	public static void main(String[] args) {
		/*String example = "public void m(){\n" +
				"<<<<<<< /Users/paolaaccioly/Desktop/Teste/jdimeTests/left/Example.java\n" +
				"        int a1;\n" +
				"||||||| /Users/paolaaccioly/Desktop/Teste/jdimeTests/base/Example.java\n" +
				"        int a;\n" +
				"=======\n" +
				"            int a;\n" +
				">>>>>>> /Users/paolaaccioly/Desktop/Teste/jdimeTests/right/Example.java\n" +
				"        int b;\n" +
				"        int c;\n" +
				"<<<<<<< /Users/paolaaccioly/Desktop/Teste/jdimeTests/left/Example.java\n" +
				"        int d1;\n" +
				"||||||| /Users/paolaaccioly/Desktop/Teste/jdimeTests/base/Example.java\n" +
				"        int d;\n" +
				"=======\n" +
				"        int d2;\n" +
				">>>>>>> /Users/paolaaccioly/Desktop/Teste/jdimeTests/right/Example.java\n" +
				"    }";
		String example2 = "hello world";
		System.out.println(example2.split("mamae")[0]);*/
		/*String s = "<<<<<<< /Users/paolaaccioly/Documents/testeConflictsAnalyzer/conflictsAnalyzer/fstmerge_tmp1437435093749/fstmerge_var1_6882939852718786152\n" +
				"		int x;" +
				"||||||| /Users/paolaaccioly/Documents/testeConflictsAnalyzer/conflictsAnalyzer/fstmerge_tmp1437435093749/fstmerge_base_7436445259957106246\n" +
				"=======\n" +
				"		int y;\n"+
				">>>>>>> /Users/paolaaccioly/Documents/testeConflictsAnalyzer/conflictsAnalyzer/fstmerge_tmp1437435093749/fstmerge_var2_5667963733764531246\n";
	*/
	}

}
