package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import main.MergeCommit;
import util.Utils;

public class MergeConflicter {

	private String REPO;
	
	// Change this line according to the language setting of Git on the computer.
	private final String COMMIT_MESSAGE = "CONFLICT (content): Merge conflict in ";

	/**
	 * Does a git merge
	 * 
	 * @return true if there were conflicting files, false otherwise
	 */
	public ArrayList<MergeCommit> doMerge(String repoPath) {
		System.out.println("FINDING CONFLICTS IN MERGES...");
		REPO = repoPath;
		ArrayList<MergeCommit> mergeCommits = new ArrayList<MergeCommit>();
		ArrayList<String> conflictShas = new ArrayList<String>();
		ArrayList<String> allCommits = getAllCommits(repoPath, false);
		for (String mergeCommit : getAllCommits(repoPath, true)) {
			try {
				String ancestrals = Utils.readScriptOutput("getAncestralCommits " + REPO + " " + mergeCommit, true)
						.readLine();
				String leftCommit = ancestrals.substring(0, 7);
				String rightCommit = ancestrals.substring(8, 15);
				
				for(String commit : allCommits) {
					if(commit.startsWith(leftCommit))
						leftCommit = commit;
					else if(commit.startsWith(rightCommit))
						rightCommit = commit;
				}
				
				String branchName = "tempbranch";

				BufferedReader br = Utils.readScriptOutput(
						"mergeHistorical " + REPO + " " + leftCommit + " " + rightCommit + " " + branchName, true);

				String line;
				final String conflictingLinePattern = COMMIT_MESSAGE;
				while ((line = br.readLine()) != null) {
					if (line.startsWith(conflictingLinePattern) && line.endsWith(".java")) {
						if (!conflictShas.contains(mergeCommit)) {
							System.out.println("CONFLICT FOUND");
							conflictShas.add(mergeCommit);
							MergeCommit mc = new MergeCommit();
							mc.setSha(mergeCommit);
							mc.setParent1(leftCommit);
							mc.setParent2(rightCommit);
							mc.setParentsAreDifferent(true);
							mc.setDate(new Date((long) 1000));
							System.out.println("ADDING MC: " + mc.getSha());
							mergeCommits.add(mc);
							break;
						}
					}
				}
				Utils.readScriptOutput("abortMerge " + REPO, false);
				Utils.readScriptOutput("deleteBranch " + REPO + " " + branchName, false);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return mergeCommits;
	}

	
	private ArrayList<String> getAllCommits(String repoPath, boolean onlyMergeCommits) {
		ArrayList<String> shas = new ArrayList<String>();
		try {
			BufferedReader br;
			if(onlyMergeCommits)
				br = Utils.readScriptOutput("getCommits " + repoPath, true);
			else
				br = Utils.readScriptOutput("getAllCommits " + repoPath, true);

			String commitSHA;
			while ((commitSHA = br.readLine()) != null) {
				shas.add(commitSHA);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return shas;
	}

}
