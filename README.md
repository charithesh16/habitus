[![Build Status](https://github.com/clulab/habitus/workflows/Habitus%20CI/badge.svg)](https://github.com/clulab/habitus/actions)
[![Docker Version](https://shields.io/docker/v/clulab/habitus?sort=semver&label=docker&logo=docker)](https://hub.docker.com/r/clulab/habitus/tags)

# HABITUS

This repository contains CLU lab's NLP software for the DARPA HEURISTICS project, which is part of the [HABITUS program](https://www.darpa.mil/program/habitus).

## 1. Requirements

This software requires:
- Java 8
- sbt 1.x

## 2. Variable reading

This component reads for values assigned to variables that are important for crop modeling such as planting date and fertilizer usage. For example, from the sentence *"Sowing between October 4 and October 14 was optimal."* our software extracts the variable *"sowing"* with the value being the date range *"between October 4 and October 14"*, which is normalized to *"XXXX-10-04 -- XXXX-10-14"*.

This component can be used in three different ways, as described below.

### 2.1. Read-eval-print loop (REPL)

We provide an interactive REPL interface, in which users can type natural language and inspect the structured output produced by the variable reader code. To call it, type:

```
./var-shell
```

For example, typing the sentence *"Farmers’ sowing dates ranged from 14 to 31 July for the WS and from 3 to 11 March for the DS."* (without the quotes), produces the following output:

```
...
events:
List(Assignment, Event) => sowing dates ranged from 14 to 31 July for the WS and from 3 to 11 March
	------------------------------
	Rule => Assignment-range-1
	Type => EventMention
	------------------------------
	trigger => ranged
	variable (Variable, Entity) => sowing dates
	value (Value, Entity) => from 3 to 11 March
	------------------------------

List(Assignment, Event) => sowing dates ranged from 14 to 31 July
	------------------------------
	Rule => Assignment-range-1
	Type => EventMention
	------------------------------
	trigger => ranged
	variable (Variable, Entity) => sowing dates
	value (Value, Entity) => from 14 to 31 July
	------------------------------
```

### 2.2. Batch mode

The same code can be called in batch mode with the command:

```
./var-read
```

This command expects a collection of documents, each saved as a `.txt` file in the directory `in/`. The software produces its output in the directory `out/`, using two formats. The first output files is called `mentions.tsv`, and it contains a tab-separated output. Similarly, it produces a `mentions.json` file, which contains the same output in JSON format, which might be more suitable for programmatic ingestion. As a simple example, let's assume that the `in/` directory contains a single file called `1.txt`, which contains the same text as the above example:

> Farmers’ sowing dates ranged from 14 to 31 July for the WS and from 3 to 11 March for the DS.

Running the `./var-read` command produces two files in the `out/` directory: `mentions.tsv` and `mentions.json`, where the former contains:

```
sowing dates    from 3 to 11 March      XXXX-03-03 -- XXXX-03-11        Farmers ’ sowing dates ranged from 14 to 31 July for the WS and from 3 to 11 March for the DS . 1.txt   N/A     N/A     N/A     N/A     N/A     N/A     N/A     N/A     N/A
sowing dates    from 14 to 31 July      XXXX-07-14 -- XXXX-07-31        Farmers ’ sowing dates ranged from 14 to 31 July for the WS and from 3 to 11 March for the DS . 1.txt   N/A     N/A     N/A     N/A     N/A     N/A     N/A     N/A     N/A
```

The `mentions.json` file contains:

```
[
  {
    "variableText" : "sowing dates",
    "valueText" : "from 3 to 11 March",
    "valueNorm" : "XXXX-03-03 -- XXXX-03-11",
    "sentenceText" : "Farmers ’ sowing dates ranged from 14 to 31 July for the WS and from 3 to 11 March for the DS .",
    "inputFilename" : "1.txt",
    "mostFreqLoc0Sent" : "N/A",
    "mostFreqLoc1Sent" : "N/A",
    "mostFreqLoc" : "N/A",
    "mostFreqDate0Sent" : "N/A",
    "mostFreqDate1Sent" : "N/A",
    "mostFreqDate" : "N/A",
    "mostFreqCrop0Sent" : "N/A",
    "mostFreqCrop1Sent" : "N/A",
    "mostFreqCrop" : "N/A"
  },
  {
    "variableText" : "sowing dates",
    "valueText" : "from 14 to 31 July",
    "valueNorm" : "XXXX-07-14 -- XXXX-07-31",
    "sentenceText" : "Farmers ’ sowing dates ranged from 14 to 31 July for the WS and from 3 to 11 March for the DS .",
    "inputFilename" : "1.txt",
    "mostFreqLoc0Sent" : "N/A",
    "mostFreqLoc1Sent" : "N/A",
    "mostFreqLoc" : "N/A",
    "mostFreqDate0Sent" : "N/A",
    "mostFreqDate1Sent" : "N/A",
    "mostFreqDate" : "N/A",
    "mostFreqCrop0Sent" : "N/A",
    "mostFreqCrop1Sent" : "N/A",
    "mostFreqCrop" : "N/A"
  }
]
```

The description of the columns in the `.tsv` file (or the equivalent fields in the `.json` file is as follows:
- "variableText" : The trigger word/phrase in the sentence which created this extraction.
 - valueText" : The part of sentence which was recognized to contain the intended output of the trigger.
 - "valueNorm" : The normalized values of the valueText. In case of of dates/date-ranges, the dates in text format is convert to the format YYYY-MM-DD
 -    "sentenceText" : The particular sentence in which this trigger was found.
 -    "inputFilename" : Name of the file which contained the sentenceText,
 -    "mostFreqLoc0Sent" : Most frequently mentioned location within the same sentence as that of the trigger (e.g.,Senegal). This context is helpful in understanding which location, if any, that particular sentence is talking about.
 -    "mostFreqLoc1Sent" : Most frequently mentioned location (e.g.,Senegal)found within 1 sentences of the trigger . This context is helpful in understanding which location, if any, that particular sentence is talking about.
 -    "mostFreqLoc" : Most frequently mentioned location overall in the file. This context is helpful in understanding which location, if any, that particular file is talking about.
 -    "mostFreqDate0Sent" : Most frequent date (e.g,1995) mentioned within the same sentence as that of the trigger . This context is helpful in understanding what time frame, if any, that particular sentence is talking about.
 -    "mostFreqDate1Sent" :  Most frequent date found within 0 sentences of the trigger. This context is helpful in understanding what time frame, if any, that particular sentence is talking about
 -    "mostFreqDate" : Most frequent date found within 0 sentences of the trigger. This context is helpful in understanding what time frame, if any, that particular file is talking about
 -    "mostFreqCrop0Sent" : Most frequent crop (e.g.,rice) mentioned within the same sentence as that of the trigger . This context is helpful in understanding what crop, if any, that particular sentence is talking about.
 -    "mostFreqCrop1Sent" :  Most frequent crop mentioned within the same sentence as that of the trigger . This context is helpful in understanding what crop, if any, that particular sentence is talking about.
 -    "mostFreqCrop" : Most frequently mentioned crop overall in the file. This context is helpful in understanding which crop, if any, that particular file is talking about.

### 2.3. Programmatic access

The key class for variable reading is [`org.clulab.habitus.variables.VariableProcessor`](https://github.com/clulab/habitus/blob/main/src/main/scala/org/clulab/habitus/variables/VariableProcessor.scala).
Instantiate it with the `apply` method, e.g.: `val vp = VariableProcessor`. The key method is `parse`, which produces a tuple with four elements as follows:

1. The first tuple element is a `Document`, which contains all sentences extracted from the corresponding text as well as various NLP preprocessing such as part-of-speech (POS) tagging, dependency parsing, and semantic roles.
2. The second element is a list of *all* extractions from this document.
3. The third element is the actual list of event mentions, where each mention associates one variable with one value (see example above).
4. The last element is a histogram of context elements (e.g., locations, years) and distance from event mentions.

For an example on how these data structures are used, take a look at the method [`org.clulab.habitus.variables.VariableReader.run`](https://github.com/clulab/habitus/blob/main/src/main/scala/org/clulab/habitus/variables/VariableReader.scala#L25).

### 2.4. Adding new regions

To contextualize extracted mentions, we find locations most likely associated with them in text. In addition to outputting the name of a location provided directly in text, we attempt to provide the country the location is in. We do that by making use of [the GeoNames geographical database](https://download.geonames.org/export/dump/) files. With the database being quite extensive, we only add the countries we are working with in a given project.

To add a new country file, follow these steps:

- run the `get_essential_region_info_from_geonames.py` script in the `/python/processing_geonames/` directory to preprocess the new geonames files:

```bash
python get_essential_region_info_from_geonames.py <path/to/directory/containing/geonames/files> <path/to/output/directory>
```
- add the name of the new country with its country code to the resulting `.tsv` file in the following format: `country\tcode`, e.g., `Senegal SN`. The country code is the same as the file name for a given country, e.g., `SN` for `Senegal`.

Geonames files cover a lot of locations, but are not exhaustive. Some locations may need to be added to location tsv.

For Uganda (UG.tsv), the list of locations was created based on a short list provided by a domain expert and a [document related to mining in Uganda](https://www.ugandainvest.go.ug/uia/images/Download_Center/SECTOR_PROFILE/Mining_Sector_Profile.pdf). The coordinates of some locations were added based on the location listing in https://mapcarta.com/, some were added based on the larger location unit that the given location is part of as listed in http://www.lcmt.org/ (e.g., Kaabong Tc is listed as part of Kaabong District, so for Kaabong Tc entry in the UG.tsv, Kaabong District coordinates are used), or based on text of documents related to the area (e.g., "Mica occurs at Morulem in Abim district; Lunya in Mukono district; Omwodulum in Lira district and Paimol, Parobong, Kacharalum, Agili, Akwanga, Achumo, Kukor, Labwordwong, Namokora, Naam and Okora in Pader district."---based on this text, we determine the district each unknown location is in and use the coordinates for that district).

The procedure of adding coordinates may not be the same for different countries. Some locations (e.g., rivers) may not have coordinates in the file.

## 3. Reading for propositional attitudes

This component reads for statements such as WHO believes WHAT. TODO Mihai.
