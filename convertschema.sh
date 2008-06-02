#! /bin/bash
java -cp ../common/lib/saxon/saxon9.jar net.sf.saxon.Transform -s:schema.xmi -xsl:../jgralab/src/de/uni_koblenz/jgralab/utilities/xmi2tgschema/XMI2TGSchema.xsl -o:src/de/uni_koblenz/jgstreetmap/OsmSchema.tg tool=ea schemaName=de.uni_koblenz.jgstreetmap.osmschema.OsmSchema
