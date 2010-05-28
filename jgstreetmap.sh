#! /bin/bash
cd /Users/riediger/src/ist/local/jgstreetmap
export JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home
/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home/bin/java -d32 -Xmx1000M -cp build/jar/jgstreetmap.jar:../jgralab/build/jar/jgralab.jar de.uni_koblenz.jgstreetmap.JGStreetMap $*
