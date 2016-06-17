package normalization

class ComputeNormTable {
	Map<String, Map<String, Integer>> conflicts
	Map<String, Map<String, Integer>> changes
	Map<String, Map<String, Double>> normalizedData
	Map<String, Double> total

	public void computeTable(String changesTable, String conflictsTable){
		Map<String, Integer> totalChanges = this.readChanges(changesTable)
		Map<String, Integer> totalConflicts = this.readConflicts(conflictsTable)
		this.computeNormTable()
		this.total = this.processProjectNormalization(totalChanges, totalConflicts)
		this.printNormTable()
	}

	private Map<String, Integer> readConflicts(String conflictsTable){
		this.conflicts = new HashMap<String, Map<String, Integer>>()
		File file = new File(conflictsTable)
		file.eachLine {
			if(!it.contains("Conflicting_Scenarios")){
				String[] tokens = it.split(",")
				String name = tokens[0]
				if(this.changes.containsKey(name)){
					Map<String, Integer> data = this.processConflictsRow(tokens)
					this.conflicts.put(name, data)
				}
			}
			
		}
		Map<String, Integer> totalConflicts = this.processTotalConflicts(this.conflicts)
		return totalConflicts
	}
	
	private Map<String, Integer> processTotalConflicts(HashMap<String, Map<String, Integer>> conflicts){
		Map<String, Integer> result = new HashMap<String, Integer>()
		int modifierList, implementList, editSameMC, addSameFd, editSameFd, sameSignatureCM, extendsList = 0
		for(String s in conflicts.keySet()){
			Map<String, Integer> project = conflicts.get(s)
			modifierList = modifierList + project.get("ModifierList")
			implementList = implementList + project.get("ImplementList")
			editSameMC = editSameMC + project.get("EditSameMC")
			addSameFd = addSameFd + project.get("AddSameFd")
			editSameFd = editSameFd + project.get("EditSameFd")
			sameSignatureCM = sameSignatureCM + project.get("SameSignatureCM")
			extendsList = extendsList + project.get("ExtendsList")
		}
		result.put("ModifierList", modifierList)
		result.put("ImplementList", implementList)
		result.put("EditSameMC", editSameMC)
		result.put("AddSameFd", addSameFd)
		result.put("EditSameFd", editSameFd)
		result.put("SameSignatureCM", sameSignatureCM)
		result.put("ExtendsList", extendsList)
		
		return result
	} 
	
	private Map<String, Integer> processConflictsRow(String[] tokens){
		Map<String, Integer> result = new HashMap<String, Integer>()
		ArrayList<String> row = Arrays.asList(tokens)
		row.remove(0)
		ArrayList<Integer> values = new ArrayList<Integer>()
		for(String value in row){
			values.add(Integer.parseInt(value))
		}
		result.put("ModifierList", values[2])
		result.put("ImplementList", values[10])
		result.put("EditSameMC", values[14])
		result.put("AddSameFd", values[18])
		result.put("EditSameFd", values[22])
		result.put("SameSignatureCM", values[26])
		result.put("ExtendsList", values[30])
		return result
	}

	private Map<String, Integer> readChanges(String conflictsTable){
		this.changes = new HashMap<String, Map<String, Integer>>()

		File file = new File(conflictsTable)
		file.eachLine {
			if(!it.contains("NumberOfScenarios")){
				String[] tokens = it.split(", ")
				String name = tokens[0]
				Map<String, Integer> data = this.processChangesRow(tokens)
				this.changes.put(name, data)
			}

		}
		Map<String, Integer> total = this.processTotalChanges(this.changes)
		return total
	}
	
	private Map<String, Integer> processTotalChanges( HashMap<String, Map<String, Integer>> changes){
		Map<String, Integer> result = new HashMap<String, Integer>()
		int modifiers, implementsList, fieldDecl, extendsList, methodDecl, constructorDecl,  
		changesInsideMethodsChunk, changesInsideMethodsLines = 0
		for(String s in changes.keySet()){
			Map<String, Integer> project = changes.get(s)
			modifiers = modifiers + project.get("Modifiers")
			implementsList = implementsList + project.get("ImplementsList")
			fieldDecl = fieldDecl + project.get("FieldDecl")
			extendsList = extendsList + project.get("ExtendsList")
			methodDecl = methodDecl + project.get("MethodDecl")
			constructorDecl = constructorDecl + project.get("ConstructorDecl")
			changesInsideMethodsChunk = changesInsideMethodsChunk + project.get("ChangesInsideMethodsChunk")
			changesInsideMethodsLines = changesInsideMethodsLines + project.get("ChangesInsideMethodsLines")
		}	
		result.put("Modifiers", modifiers)
		result.put("ImplementsList",implementsList )
		result.put("FieldDecl", fieldDecl)
		result.put("ExtendsList", extendsList)
		result.put("MethodDecl", methodDecl)
		result.put("ConstructorDecl", constructorDecl)
		result.put("ChangesInsideMethodsChunk", changesInsideMethodsChunk)
		result.put("ChangesInsideMethodsLines", changesInsideMethodsLines)
		return result
	}
	
	private Map<String, Integer> processChangesRow(String[] tokens){
		Map<String, Integer> result = new HashMap<String, Integer>()
		ArrayList<String> row = Arrays.asList(tokens)
		row.remove(0)
		ArrayList<Integer> values = new ArrayList<Integer>()
		for(String value in row){
			values.add(Integer.parseInt(value))
		}
		result.put("Modifiers", values.get(1))
		result.put("ImplementsList", values.get(3))
		result.put("FieldDecl", values.get(4))
		result.put("ExtendsList", values.get(5))
		result.put("MethodDecl", values.get(6))
		result.put("ConstructorDecl", values.get(7))
		result.put("ChangesInsideMethodsChunk", values.get(9))
		result.put("ChangesInsideMethodsLines", values.get(10))

		return result
	}

	private void computeNormTable(){
		this.normalizedData = new HashMap<String, Map<String, Integer>>()
		for(String project in this.changes.keySet()){
			Map<String, Integer> normData = this.processProjectNormalization(this.changes.get(project), this.conflicts.get(project))
			this.normalizedData.put(project, normData)
		}
	}

	private Map<String, Integer> processProjectNormalization(Map<String, Integer> changes, Map<String, Integer> conflicts){
		Map<String, Integer> result = new HashMap<String, Integer>()
		
		//EditSameMC
		double nEditSameMCChunks = conflicts.get("EditSameMC")/changes.get("ChangesInsideMethodsChunk")
		double nEditSameMCLines = conflicts.get("EditSameMC")/changes.get("ChangesInsideMethodsLines")
		double sum = changes.get("MethodDecl") + changes.get("ConstructorDecl")
		double nEditSameMC = conflicts.get("EditSameMC")/sum
		
		result.put("nEditSameMCChunks", nEditSameMCChunks)
		result.put("nEditSameMCLines", nEditSameMCLines)
		result.put("nEditSameMC", nEditSameMC)
		
		//sum changes outside methods
		double sumOM = changes.get("Modifiers") + changes.get("ImplementsList") + changes.get("FieldDecl") + changes.get("ExtendsList")
		
		//ModifierList
		double nModifierList = conflicts.get("ModifierList")/changes.get("Modifiers")
		double nModifierListOM = conflicts.get("ModifierList")/sumOM
		result.put("nModifierList", nModifierList)
		result.put("nModifierListOM",nModifierListOM )
		
		//ImplementList
		int nImplementList = conflicts.get("ImplementList")/changes.get("ImplementsList")
		int nImplementListOM = conflicts.get("ImplementList")/sumOM
		result.put("nImplementList", nImplementList)
		result.put("nImplementListOM", nImplementListOM)
		
		//AddSameFd
		double nAddSameFd = conflicts.get("AddSameFd")/changes.get("FieldDecl")
		double nAddSameFdOM = conflicts.get("AddSameFd")/sumOM
		result.put("nAddSameFd", nAddSameFd)
		result.put("nAddSameFdOM", nAddSameFdOM)
		
		//EditSameFd
		double nEditSameFd = conflicts.get("EditSameFd")/changes.get("FieldDecl")
		double nEditSameFdOM = conflicts.get("EditSameFd")/sumOM
		result.put("nEditSameFd", nEditSameFd)
		result.put("nEditSameFdOM", nEditSameFdOM)
		
		//SameSignatureCM
		double nSameSignatureCM = conflicts.get("SameSignatureCM")/sum
		result.put("nSameSignatureCM", nSameSignatureCM)
		
		//ExtendsList
		double nExtendsList = conflicts.get("ExtendsList")/changes.get("ExtendsList")
		double nExtendsListOM = conflicts.get("ExtendsList")/sumOM
		result.put("nExtendsList",nExtendsList )
		result.put("nExtendsListOM",nExtendsListOM )
		
		return result
	}

	private void printNormTable(){
		File file = new File("NormalizedData.csv")
		file.delete()
		String header = "Project, nEditSameMCChunks, nEditSameMCLines, nEditSameMC, nModifierList, nModifierListOM, " +
		"nImplementList, nImplementListOM, nAddSameFd, nAddSameFdOM, nEditSameFd, nEditSameFdOM, " +
		"nSameSignatureCM, nExtendsList, nExtendsListOM\n"
		
		file.append(header)
		for(String projectName in this.normalizedData.keySet()){
			String line = projectName + ", "
			Map<String, Integer> projectData = this.normalizedData.get(projectName)
			line = line + projectData.get("nEditSameMCChunks") + ", " +  projectData.get("nEditSameMCLines") + ", " +
			projectData.get("nEditSameMC") + ", " + projectData.get("nModifierList") + ", "  + projectData.get("nModifierListOM") + ", " +
			projectData.get("nImplementList") + ", " + projectData.get("nImplementListOM") + ", " + projectData.get("nAddSameFd") + ", "+ 
			projectData.get("nAddSameFdOM") + ", " + projectData.get("nEditSameFd") + ", " + projectData.get("nEditSameFdOM") + ", " +
			projectData.get("nSameSignatureCM") + ", " + projectData.get("nExtendsList") + ", " + projectData.get("nExtendsListOM") + "\n"
			file.append(line)
		}
		
		String total = "TOTAL, " + this.total.get("nEditSameMCChunks") + ", " + this.total.get("nEditSameMCLines") + ", " +
			this.total.get("nEditSameMC") + ", " + this.total.get("nModifierList") + ", "  + this.total.get("nModifierListOM") + ", " +
			this.total.get("nImplementList") + ", " + this.total.get("nImplementListOM") + ", " + this.total.get("nAddSameFd") + ", "+ 
			this.total.get("nAddSameFdOM") + ", " + this.total.get("nEditSameFd") + ", " + this.total.get("nEditSameFdOM") + ", " +
			this.total.get("nSameSignatureCM") + ", " + this.total.get("nExtendsList") + ", " + this.total.get("nExtendsListOM") + "\n"
		file.append(total)
	}

	public static void main(String[] args){
		ComputeNormTable c = new ComputeNormTable()
		c.computeTable("/Users/paolaaccioly/Documents/Doutorado/workspace_fse/conflictsAnalyzer/projectsChanges.csv",
				"/Users/paolaaccioly/Documents/Doutorado/workspace_fse/conflictsAnalyzer/projectsPatternData.csv")
	
	}

}
