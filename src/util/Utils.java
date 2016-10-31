package util;
/*
Copyright (C) 2016 Isak Eriksson, Patrik Wållgren

This file is part of ResolutionsAnalyzer.

    ResolutionsAnalyzer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ResolutionsAnalyzer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ResolutionsAnalyzer.  If not, see <http://www.gnu.org/licenses/>.
*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Utils {

	public static ArrayList<String> getAllMergeCommits(String repo) {
		ArrayList<String> mergeCommits = new ArrayList<String>();
		try {
			Process p = Runtime.getRuntime().exec("scripts/getCommits " + repo);
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String commitSHA;
			while((commitSHA = br.readLine()) != null) {
				mergeCommits.add(commitSHA);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mergeCommits;
	}
	
	public static BufferedReader readScriptOutput(String command, boolean readOutput) throws IOException {
		Process p; 
		if(!readOutput) {
			p = Runtime.getRuntime().exec("scripts/" + command + " &> /dev/null");
			try {
				p.waitFor(10000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		p = Runtime.getRuntime().exec("scripts/" + command + " 2> /dev/null");
		
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		try {
			p.waitFor(3000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return br;
	}
	
	public static BufferedReader readCommandOutput(String command) throws IOException {
		Process p = Runtime.getRuntime().exec(command);
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new BufferedReader(new InputStreamReader(p.getInputStream()));
	}
	
	public static void checkoutCommit(String repo, String commit) throws IOException {
		readScriptOutput("checkoutCommit " + repo + " " + commit, false);
	}
}
