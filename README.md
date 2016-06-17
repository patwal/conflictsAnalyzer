The Conflict Analyzer tool is part of an experiment infrastructure that aims to analyze conflict patterns and their frequency. 

More information here: http://twiki.cin.ufpe.br/twiki/bin/view/SPG/ConflictPatterns

If you have any questions please contact:

Paola Accioly - prga at cin.ufpe.br

Paulo Borba - phmb at cin.ufpe.br

Guilherme Cavalcanti - gjcc at cin.ufpe.br

Install and run: 

In order to run the conflicts analyzer you will need to have Java 7 or higher, groovy, maven and R installed in your machine. 
After that, you will need to clone the required projects from github and import then inside Eclipse IDE according 
to the instructions described below:

1- clone and build gitminer

    git clone https://github.com/patwal/gitminer.git
    cd gitminer
    mvn clean compile package assembly:single
    cd ..

2- clone GremlinQuery and switch to right branch

    git clone https://github.com/patwal/GremlinQuery.git

3-clone featurehouse (ssmerge) 

    git clone https://github.com/patwal/featurehouse.git

4- clone conflictsanalyzer

    git clone https://github.com/patwal/conflictsAnalyzer.git

After cloning these 4 projects (gitminer, GremlinQuery, featurehouse, and conflictsAnalyzer), you need to import 3 projects
inside Eclipse IDE

5-Open Eclipse

6- Import project GremlinQuery

import ->maven->existing maven projects
select GremlinQuery folder and click open and then finish
install required plugins
restart eclipse

6.1 - if you have problems with groovy compiler mismatch do this

right click on project’s folder -> groovy-> fix compiler mismatch problems

right click on project’s folder -> maven -> update project

7- import featurehouse

import->general-> existing projects into workspace

select featurehouse folder click open, check that all 4 projects are selected (CIDE_generateAST, CIDE2_ast, 
fstcomp, fstgen, fstmerge), and click finish

8-import conflictsAnalyzer

import-> existing projects into workspace

select conflictsAnalyzer folder, click open and then finish

9- Edit properties files and run conflictsAnalyzer project

Edit projectList file with the list of projects you wish to analyze, following the file pattern with one project per line

Edit configuration.properties file with the following information:

-gitminer.path, should be set to the path of gitminer project

-downloads.path, should be set to the path where you want to download projects revisions

-github.login, your github login

-github.password, your github password

-github.email, your github email 

-github.token, your github token to allow your login to make multiple requests to Github's API. Instructions to get your token
here https://help.github.com/articles/creating-an-access-token-for-command-line-use/

Edit importpath and exportpath variables from resultscript.r with the following information:

-importpath, should be set to the conflictsanalyzer folder

-exportpath, should be set to where you want to store the html with the results

run RunStudy.java class from conflictsAnalyzer project

if you have a this problem:

    Caused by: groovy.lang.GroovyRuntimeException: Conflicting module versions. Module [groovy-all is loaded in version 2.3.7
    and you are trying to load version 2.0.7

open the file pom.xml from GremlinQuery, edit the groovy-all property with the version number of the groovy compiler from your
workspace, save, and then right click GremlinQuery project -> maven-> update project

try to run RunStudy.groovy again
