#! /bin/bash
java -Xmx1000M -cp build/jar/jgstreetmap.jar:../jgralab/build/jar/jgralab.jar de.uni_koblenz.jgstreetmap.JGStreetMap $*
