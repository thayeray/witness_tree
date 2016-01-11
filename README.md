# witness_tree
Helps you find the witness trees in your DeedMapper files and convert them to GIS readable points. Written in Java.

Tract descriptions often used trees as boundary points during the colonial period in the US. Land patents, the first
deed on a plot of land, are therefore a good source of information about the abundance of tree species at the time of
settlement.  This program reads the output .mbl and .kml files from the DeedMapper program. It then converts those files
into two tab delimited .txt files that are easily imported into common geographic information systems (GIS) software.

The converted files are a "_flat.txt" file and a "_geo.txt" file. The flat file contains all of the general information 
about the parcel, for example the information contained in the typ, id, decl, ref, con, dat, rec, frm, to, re, lbl, adj, 
and loc fields. Two editable text boxes are included that allow the user to search for custom field names in addition to
the standard DeedMapper field names. These begin with "! ". The first text box looks for single line custom field names,
the second will combine all lines that begin with "! " and follow the multi-line field name.

The geo file contains the details about the individual tract description courses. The geo file also joins the latitude 
and longitude coordinates from the .kml file together with the corresponding course in the .mbl tract description, i.e.
the pt, lc, ln or lm with its direction, distance and comment. Note the tract descriptions are assumed to begin at a point, be 
followed by the courses, with the last course ending at the beginning point.

The tract course comments in the .mbl file is where the witness tree information is stored. An editable text box is 
provided with some sample search terms. The user should edit this list to meet their own needs. The program will then 
search for each word or phrase on each line of the list in the .mbl course comments and the results can be found in the 
"FoundTerms" field of the output geo file.

The output files can be imported into GIS using the GIS program's open text file functionality and setting the geometry
to be the KML_x and KML_y fields. ESRI software refers to this as "events". The coordinate system should be set to be
the Google Spherical Mercator projection EPSG 4326 (World Geodetic System 1984).

More detailed directions for using the program, including screenshots, can be found at:
http://www.cicadagis.com/witness_tree/index.html

This program was written pro bono publico by Thayer Young for the Maryland/DC Chapter of the Nature Conservancy. 
Copyright 2016 Thayer Young, Cicada Systems GIS Consulting.
