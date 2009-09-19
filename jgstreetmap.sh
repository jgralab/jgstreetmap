#! /bin/bash
cd /Users/riediger/src/ist/local/jgstreetmap
export JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home
/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home/bin/java -Xmx1600M -cp build/jar/jgstreetmap.jar:../jgralab/build/jar/jgralab.jar de.uni_koblenz.jgstreetmap.JGStreetMap $*
