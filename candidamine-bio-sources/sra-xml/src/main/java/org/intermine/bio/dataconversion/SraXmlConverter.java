package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2019 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


/**
 * 
 * @author
 */
public class SraXmlConverter extends BioFileConverter
{
    //
//
    private  String dataSetTitle = null;
    private  String dataSourceName = null;
    private String dataSet = null;

    private String taxonId  = null;
    private String refOragism = null;

    private  String fileEnding  = null;


    private Map<String, String> strains = new HashMap<String, String>();


    // all item have accession number
    HashMap<String,Item> sraObjects = new HashMap<>();


    public void setDataSet(String dataSetTitle)
    {
        this.dataSetTitle = dataSetTitle;
    }

    public void setDataSource(String dataSource)
    {
        this.dataSourceName = dataSource;
    }

    public void setTaxonId(String taxonId)
    {
        this.taxonId = taxonId;
    }

    public void setFileEnding(String fileEnding)
    {
        this.fileEnding = fileEnding;
    }

    private void setupDataset()
    {
        String dataSource = null;
        if (StringUtils.isNotEmpty(dataSourceName) && StringUtils.isNotEmpty(dataSetTitle)) {
            dataSource = getDataSource(dataSourceName);
            dataSet = getDataSet(dataSetTitle, dataSource);
            setStoreHook(new BioStoreHook(getModel(), dataSet, dataSource, getSequenceOntologyRefId()));
        }

    }
    // we will process all files we have
    /**
     * The Organism item created from the taxon id passed to the constructor.
     * @param taxonId NCBI taxonomy id of organism to create
     * @return the refId representing the Organism Item
     */
    public String getStrain(String taxonId) {
        String refId = strains.get(taxonId);
        if (refId == null) {
            Item strain = createItem("CStrain");
            strain.setAttribute("taxonId", taxonId);
            strain.setReference("organism",refOragism);
            try {
                store(strain);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store strain with taxonId: " + taxonId, e);
            }
            refId = strain.getIdentifier();
            strains.put(taxonId, refId);
        }
        return refId;
    }


    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public SraXmlConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // create data set once per source
        if(dataSet == null) {
            setupDataset();
        }
        // set main Organism for this source
        //
        refOragism = getOrganism(taxonId);


        File currentFile = getCurrentFile();
        Pattern filePattern = Pattern.compile("^(\\S+).xml");
        Matcher matcher = filePattern.matcher(currentFile.getName());
        if (matcher.find()) {
            String sraRunAcc = matcher.group(1);
            System.err.println("Loading data from SRA run  " + sraRunAcc + ", From File " + currentFile.getName());
            processXmlFile(currentFile.getPath());
        }
    }




    // #############################################################
    static final String EXPERIMENT_NODE = "EXPERIMENT";
    static final String STUDY_NODE = "STUDY";
    static final String SAMPLE_NODE = "SAMPLE";
    static final String RUN_NODE = "RUN";
    static final String RUN_SET = "RUN_SET";

    private void processXmlFile(String xmlFileName) throws ParserConfigurationException, IOException, SAXException, ObjectStoreException {
        DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFileName);


        doc.getDocumentElement().normalize();


        NodeList experiment_packages = doc.getElementsByTagName("EXPERIMENT_PACKAGE");

        for(int i =0; i <  experiment_packages.getLength();i++ ) {
            // we could have more than one EXPERIMENT_PACKAGE
            // but each one has one EXPERIMENT,STUDY , sample and 1 or more than one RUN
            processExperimentPackage(experiment_packages.item(i));

        }

       /* NodeList expNodes = doc.getElementsByTagName(EXPERIMENT_NODE);
        processExperiments(expNodes);

        NodeList studyNodes = doc.getElementsByTagName(STUDY_NODE);
        // processStudies(studyNodes);


        NodeList sampleNodes = doc.getElementsByTagName(SAMPLE_NODE);
        // processSamples(sampleNodes);


        NodeList runNodes = doc.getElementsByTagName(RUN_NODE);
        processRunsNodes(runNodes);*/
    }


    // we only need this for further referenece
    // any more ??
    Item currentSRAExperiment = null;
    boolean toStoreCurrentSRAExperiment = false;
    private void processExperimentPackage(Node item) throws ObjectStoreException {
        for(int j =0; j <  item.getChildNodes().getLength();j++ ) {
            Node childNode = item.getChildNodes().item(j);
            String childNodeName = childNode.getNodeName();
            if(EXPERIMENT_NODE.equals(childNodeName)) {
                // need to store this as the current experiment
                // in the node we have a ref to the study but not the sample
                // once we see the sample we add it to the experiment
                // experiment can be redundant in different files
                // this if we have diff runs of the same experiment we expect to see it again
                processExperimentNode(childNode);
            }
            if(STUDY_NODE.equals(childNodeName)) {
                // handle filling study info and call store upon it.
                processStudyNode(childNode);
            }
            if(SAMPLE_NODE.equals(childNodeName)) {
                // handle filling Sample info and call store upon it.
                processSampleNode(childNode);
            }
            if(RUN_SET.equals(childNodeName)) {
                processRunsNodes(childNode.getChildNodes());
            }
        }

        // done
        //
        if(toStoreCurrentSRAExperiment)
            store(currentSRAExperiment);
        currentSRAExperiment = null;
        toStoreCurrentSRAExperiment = false;
    }

    // ################################################################################################################
    // #################################     Sample             #######################################################
    // ################################################################################################################


    private void processSampleNode(Node sampleNode) throws ObjectStoreException {
        // [center_name, alias, accession]
        String sampleAcc = sampleNode.getAttributes().getNamedItem("accession").getNodeValue();

        Item sampleItem = sraObjects.get(sampleAcc);

        if(sampleItem == null) {
            AttributesMap attMap = null;
            boolean strainMissing = true;
            String title = null, alias = null;

            sampleItem = createItem("SRASample");
            sampleItem.setReference("organism", refOragism);
            sraObjects.put(sampleAcc, sampleItem);
            // set main accession id

            sampleItem.setAttribute("accession", sampleAcc);

            // if has alias set it
            if (sampleNode.getAttributes().getNamedItem("alias") != null) {
                alias = sampleNode.getAttributes().getNamedItem("alias").getNodeValue();
                sampleItem.setAttribute("alias", alias);
            }

            // childNodes  : [DESCRIPTION, SAMPLE_NAME, IDENTIFIERS, TITLE, SAMPLE_ATTRIBUTES, SAMPLE_LINKS]


            for (int j = 0; j < sampleNode.getChildNodes().getLength(); j++) {
                Node childNode = sampleNode.getChildNodes().item(j);
                String childNodeName = childNode.getNodeName();
                if ("SAMPLE_ATTRIBUTES".equals(childNodeName)) {
                    attMap = addSampleAttributes(sampleItem, childNode.getChildNodes());
                }
                if ("SAMPLE_NAME".equals(childNodeName)) {
                    String sampleTaxonID = getSampleTaxonId(childNode);
                    if (sampleTaxonID != null) {
                        if (sampleTaxonID.equals(taxonId)) {

                        } else {
                            String strainRef = getStrain(sampleTaxonID);
                            sampleItem.setReference("strain", strainRef);
                            strainMissing = false;
                        }
                    }
                    // else what to do
                }
                if ("TITLE".equals(childNodeName)) {
                    title = getInnerText(childNode);
                    sampleItem.setAttributeIfNotNull("title", title);
                }
            }

            // <SAMPLE_NAME>
            // <TAXON_ID>5476</TAXON_ID>
            // <SCIENTIFIC_NAME>Candida albicans</SCIENTIFIC_NAME>
            // </SAMPLE_NAME>

            // SAMPLE_ATTRIBUTE
            // Important :: strain,organism,genotype,Title
            //
            if (strainMissing) {
                // look it up
                // TODO :: strains is more complicated we need to add them but do not repeat
            }
            if (title == null) { // missing title look it up in attributes
                if (attMap.attsMap.containsKey("title")) {
                    sampleItem.setAttributeIfNotNull("title", attMap.attsMap.get("title"));
                } else if (attMap.attsMap.containsKey("Title")) {
                    sampleItem.setAttributeIfNotNull("title", attMap.attsMap.get("Title"));
                }
            }
            if (alias == null) { // or it is an ID {
                if (attMap.attsMap.containsKey("alias")) {
                    sampleItem.setAttributeIfNotNull("alias", attMap.attsMap.get("alias"));
                } else if (attMap.attsMap.containsKey("Alias")) {
                    sampleItem.setAttributeIfNotNull("alias", attMap.attsMap.get("Alias"));
                }
            }
            store(sampleItem);
        }
        currentSRAExperiment.setReference("sraSample", sampleItem);
    }

    HashMap<String,Item> sraAttributesKey = new HashMap<>();
    class AttributesMap {
        HashMap<String,String> attsMap = new HashMap<>();
        HashMap<String,String> attsUnitMap = new HashMap<>();
    }
    private AttributesMap addSampleAttributes(Item sampleItem, NodeList  sampleAttTopNodes ) throws ObjectStoreException {

        AttributesMap attributesMap = collectAttributesMap(sampleAttTopNodes);

        // process extra logic here
        // genotype
        // TODO :: build a key map to help
        if(attributesMap.attsMap.containsKey("genotype")) {
            sampleItem.setAttributeIfNotNull("genotype",attributesMap.attsMap.get("genotype"));
        } else if(attributesMap.attsMap.containsKey("genotype/variation")){
            sampleItem.setAttributeIfNotNull("genotype",attributesMap.attsMap.get("genotype/variation"));
        } else if(attributesMap.attsMap.containsKey("ca genotype")) {
            sampleItem.setAttributeIfNotNull("genotype",attributesMap.attsMap.get("ca genotype"));
        }
        // isolation source,


        addSRAAttributes(sampleItem,attributesMap.attsMap,attributesMap.attsUnitMap);


        return attributesMap;
    }

    private AttributesMap collectAttributesMap(NodeList sraAttTopNodes) {
        AttributesMap attributesMap = new AttributesMap();

        for (int i = 0; i < sraAttTopNodes.getLength(); i++) {


            Node sraAttTagValueNodes = sraAttTopNodes.item(i);
            String tag=null,value=null,unit=null;

            for (int j = 0; j < sraAttTagValueNodes.getChildNodes().getLength(); j++) {
                Node childNode = sraAttTagValueNodes.getChildNodes().item(j);

                if(childNode.getNodeName().equals("TAG")) {
                    tag =getInnerText(childNode);
                }
                if(childNode.getNodeName().equals("VALUE")) {
                    value=getInnerText(childNode);
                }
                if(childNode.getNodeName().equals("UNITS")) {
                    unit = getInnerText(childNode);

                }

            }

            if(!attributesMap.attsMap.containsKey(tag))
                attributesMap.attsMap.put(tag,value);
            if(unit!= null) {
                attributesMap.attsUnitMap.put(tag,unit);
            }
        }
        return  attributesMap;
    }

    private void addSRAAttributes(Item sraItem, HashMap<String, String> attsMap, HashMap<String, String> attsUnitMap) throws ObjectStoreException {
        for (String attkey: attsMap.keySet()) {
            Item attKeyItem = sraAttributesKey.get(attkey);
            if(attKeyItem == null) {
                attKeyItem = createItem("AttributeKey");

                attKeyItem.setAttribute("name",attkey);
                sraAttributesKey.put(attkey,attKeyItem);
                store(attKeyItem);
            }
            Item attValueItem = null;
            if(attsUnitMap.get(attkey) != null) {
                attValueItem = createItem("AttributeUValue");
                attValueItem.setAttribute("units",attsUnitMap.get(attkey));
            }
            else {
                attValueItem = createItem("AttributeValue");
            }
            attValueItem.setAttribute("value", attsMap.get(attkey));
            store(attValueItem);

            Item attKeyValueItem = createItem("SRAAttribute");
            attKeyValueItem.setReference("attributeName",attKeyItem);
            attKeyValueItem.setReference("attributeValue",attValueItem);
            attKeyValueItem.setReference("sraObject",sraItem);

            store(attKeyValueItem);
        }
    }

    private String getSampleTaxonId(Node sampleNameNode) {
        for (int j = 0; j < sampleNameNode.getChildNodes().getLength(); j++) {
            Node childNode = sampleNameNode.getChildNodes().item(j);
            if("TAXON_ID".equals(childNode.getNodeName())) {
               return  getInnerText(childNode);
            }
        }
        return null;
    }


    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^  Sample  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^




    // ################################################################################################################
    // #################################     Study              #######################################################
    // ################################################################################################################


    private void processStudyNode(Node studyNode) throws ObjectStoreException {
        // [center_name, broker_name, alias, xmlns:com, accession]


        // set main accession id
        String studyAcc = studyNode.getAttributes().getNamedItem("accession").getNodeValue();
        Item studyItem = sraObjects.get(studyAcc);

        if(studyItem == null) {
            studyItem = createItem("SRAStudy");
            sraObjects.put(studyAcc,studyItem);
            studyItem.setAttribute("accession",studyAcc);

            // if has alias set it
            if(studyNode.getAttributes().getNamedItem("alias")  != null) {
                String alias = studyNode.getAttributes().getNamedItem("alias").getNodeValue();
                studyItem.setAttribute("alias",alias);
            }
            // ChildNodes   : [DESCRIPTOR, STUDY_LINKS, IDENTIFIERS, STUDY_ATTRIBUTES]
            for(int i =0; i <  studyNode.getChildNodes().getLength();i++ ) {
                Node childNode = studyNode.getChildNodes().item(i);
                if("STUDY_LINKS".equals(childNode.getNodeName())) {
                   ArrayList<String>  pubMedIds = getPubMedsFromStudyLinkNodes(childNode.getChildNodes());
                   if(pubMedIds!= null && pubMedIds.size() > 0) {
                       ArrayList<String> pubCollection = new ArrayList<>();
                       for (String pubMedId : pubMedIds) {
                            String refId = getPub(pubMedId);
                           pubCollection.add(refId);
                       }
                       studyItem.setCollection("publications",pubCollection);
                   }
                }

                if("DESCRIPTOR".equals(childNode.getNodeName())) {
                    for(int j =0; j <  childNode.getChildNodes().getLength();j++ ) {
                        Node childInnerNode = childNode.getChildNodes().item(j);
                        if ("STUDY_TITLE".equalsIgnoreCase(childInnerNode.getNodeName())) {
                            String studyTitle = getInnerText(childInnerNode);
                            // System.err.println("STUDY_TITLE: " + studyTitle);
                            studyItem.setAttributeIfNotNull("title" , studyTitle);
                        }
                        if ("STUDY_ABSTRACT".equalsIgnoreCase(childInnerNode.getNodeName())) {
                            String studyAbstract = getInnerText(childInnerNode);
                            // System.err.println("STUDY_ABSTRACT: " + studyAbstract);
                            if(studyAbstract != null && !studyAbstract.trim().equals("")) {
                                Item description = null;
                                if(!sraObjects.containsKey(studyAbstract)) {
                                    description = createItem("Description");
                                    description.setAttribute("description",studyAbstract);
                                    store(description);
                                    sraObjects.put(studyAbstract,description);
                                }
                                description = sraObjects.get(studyAbstract);
                                studyItem.setReference("abstractText", description);
                            }

                        }
                        if ("STUDY_TYPE".equalsIgnoreCase(childInnerNode.getNodeName())) {
                            if(childInnerNode.hasAttributes()) {
                                Node studyTypeNode = childInnerNode.getAttributes().getNamedItem("existing_study_type");
                                if (studyTypeNode != null) {
                                    String studyTypeValue = studyTypeNode.getNodeValue();
                                    // System.err.println("STUDY_TYPE: " + studyTypeValue);
                                    studyItem.setAttributeIfNotNull("type" , studyTypeValue);
                                }
                            }
                        }
                    }
                }

            }
                // more ??
            store(studyItem);
        }

        currentSRAExperiment.setReference("sraStudy",studyItem);
    }
    static ArrayList<String> getPubMedsFromStudyLinkNodes(NodeList studyLinks) {
        ArrayList<String> ids = new ArrayList<>();
        for(int i =0; i <  studyLinks.getLength();i++ ) {
            Node studyLink = studyLinks.item(i);
            ids.addAll(getPubMedFromNode(studyLink.getChildNodes()));
        }
        return ids ;
    }

    private static ArrayList<String> getPubMedFromNode(NodeList studyLinkChildNodes) {
        ArrayList<String> ids = new ArrayList<>();
        for(int i =0; i <  studyLinkChildNodes.getLength();i++ ) {
            String db = null, id = null;
            Node childNode = studyLinkChildNodes.item(i);
            if("XREF_LINK".equals(childNode.getNodeName())) {
                for(int j =0; j <  childNode.getChildNodes().getLength();j++ ) {
                    Node innerNode = childNode.getChildNodes().item(j);
                    if("db".equalsIgnoreCase(innerNode.getNodeName())) {
                        db = getInnerText(innerNode);
                    }
                    if("id".equalsIgnoreCase(innerNode.getNodeName())) {
                        id = getInnerText(innerNode);
                    }
                }

                if(db.equalsIgnoreCase("pubmed"))
                {
                    ids.add(id);
                }
            }
        }

        return  ids;
    }

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^  Study  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


    // ################################################################################################################
    // #################################     Experiment         #######################################################
    // ################################################################################################################


    static  final String EXP_ATT_LIBRARY_NAME = "Library Name";

    /**
     *
     * @param expNode
     */
    private void processExperimentNode(Node expNode) throws ObjectStoreException {
        if(currentSRAExperiment != null)
            throw new UnsupportedOperationException("Something is wrong current SRAExperiment is not stored correctly");


        // set main accession id
        String experimentAcc = expNode.getAttributes().getNamedItem("accession").getNodeValue();
        currentSRAExperiment = sraObjects.get(experimentAcc);
        if(currentSRAExperiment == null) {

            currentSRAExperiment = createItem("SRAExperiment");
            sraObjects.put(experimentAcc,currentSRAExperiment);
            currentSRAExperiment.setAttribute("accession", experimentAcc);

            // if has alias set it
            if (expNode.getAttributes().getNamedItem("alias") != null) {
                String alias = expNode.getAttributes().getNamedItem("alias").getNodeValue();
                currentSRAExperiment.setAttribute("alias", alias);

            }


            //  [PLATFORM, DESIGN, PROCESSING, EXPERIMENT_ATTRIBUTES, IDENTIFIERS, TITLE, STUDY_REF, EXPERIMENT_LINKS]
            for (int j = 0; j < expNode.getChildNodes().getLength(); j++) {
                Node childNode = expNode.getChildNodes().item(j);
                if ("STUDY_REF".equals(childNode.getNodeName())) {
                    String sraStudyRef = childNode.getAttributes().getNamedItem("accession").getNodeValue();
                    // not there yet better add it later
                }
                if ("TITLE".equals(childNode.getNodeName())) {
                    String title = getInnerText(childNode);
                    currentSRAExperiment.setAttributeIfNotNull("title", title);
                }

                // DESIGN
                if ("DESIGN".equals(childNode.getNodeName())) {
                    AttributesMap attributesMap = getExperimentDetailsAttributes(childNode);
                    addExperimentDetailsAttributes(currentSRAExperiment,attributesMap.attsMap);


                }
                if ("EXPERIMENT_ATTRIBUTES".equals(childNode.getNodeName())) {
                    addExperimentAttributes(currentSRAExperiment, childNode.getChildNodes());
                }

            }
            toStoreCurrentSRAExperiment = true;
        }
    }

    private AttributesMap getExperimentDetailsAttributes(Node designNode) {
//        <DESIGN_DESCRIPTION>Stress responses of Candida albicans</DESIGN_DESCRIPTION>
//        <SAMPLE_DESCRIPTOR refname="E-MTAB-2822:04H2O2NaCl_20_C" accession="ERS523446" refcenter="University of Aberdeen">...</SAMPLE_DESCRIPTOR>
//        <LIBRARY_DESCRIPTOR>
//        <LIBRARY_NAME>04H2O2NaCl_20_C</LIBRARY_NAME>
//        <LIBRARY_STRATEGY>RNA-Seq</LIBRARY_STRATEGY>
//        <LIBRARY_SOURCE>TRANSCRIPTOMIC</LIBRARY_SOURCE>
//        <LIBRARY_SELECTION>cDNA</LIBRARY_SELECTION>
//        <LIBRARY_LAYOUT>
//        <SINGLE/>
//        </LIBRARY_LAYOUT>
//        <LIBRARY_CONSTRUCTION_PROTOCOL>

        // collect all above nodse as attribute for not
        // TODO :: maybe we can enhance this with separate object
        AttributesMap attributesMap = new AttributesMap();
        for(int i =0 ; i < designNode.getChildNodes().getLength() ;i++) {
            Node designInternalNode = designNode.getChildNodes().item(i);
            if("DESIGN_DESCRIPTION".equals(designInternalNode.getNodeName())) {
                String designDescription = getInnerText(designInternalNode);
                if(designDescription != null )
                    attributesMap.attsMap.put("Design Description",designDescription);
            } else if("LIBRARY_DESCRIPTOR".equals(designInternalNode.getNodeName())) {
                for(int j =0 ; j < designInternalNode.getChildNodes().getLength() ;j++) {
                    Node libraryInternalNode = designInternalNode.getChildNodes().item(j);
                    if("LIBRARY_NAME".equals(libraryInternalNode.getNodeName())) {
                        String libraryName = getInnerText(libraryInternalNode);
                        if(libraryName != null )
                            attributesMap.attsMap.put(EXP_ATT_LIBRARY_NAME,libraryName);
                    } else if ("LIBRARY_STRATEGY".equals(libraryInternalNode.getNodeName())) {
                        String libraryStrategy = getInnerText(libraryInternalNode);
                        if(libraryStrategy != null )
                            attributesMap.attsMap.put("Library Strategy",libraryStrategy);
                    } else if ("LIBRARY_SOURCE".equals(libraryInternalNode.getNodeName())) {
                        String librarySource = getInnerText(libraryInternalNode);
                        if(librarySource != null )
                            attributesMap.attsMap.put("Library Source",librarySource);
                    } else if ("LIBRARY_SELECTION".equals(libraryInternalNode.getNodeName())) {
                        String librarySelection = getInnerText(libraryInternalNode);
                        if(librarySelection != null )
                            attributesMap.attsMap.put("Library Selection",librarySelection);
                    } else if ("LIBRARY_LAYOUT".equals(libraryInternalNode.getNodeName())) {
                        if(libraryInternalNode.getChildNodes().getLength() > 0) {
                            Node libraryLayoutNode = libraryInternalNode.getFirstChild();
                            if("SINGLE".equals(libraryLayoutNode.getNodeName())) {
                                attributesMap.attsMap.put("Library Layout","SINGLE");
                            } else if("PAIRED".equals(libraryLayoutNode.getNodeName())) {
                                attributesMap.attsMap.put("Library Layout","PAIRED");
                            }
                        }
                    } else if ("LIBRARY_CONSTRUCTION_PROTOCOL".equals(libraryInternalNode.getNodeName())) {
                        String libraryProtocol = getInnerText(libraryInternalNode);
                        if(libraryProtocol != null )
                            attributesMap.attsMap.put("Library Construction Protocol",libraryProtocol);
                    }
                }
            }
        }
        return attributesMap;
    }

    private void addExperimentDetailsAttributes(Item expItem, HashMap<String, String> attsMap) throws ObjectStoreException {
        ArrayList<String > detialsCollection = new ArrayList<>();
        for (String attkey: attsMap.keySet()) {
            Item attKeyItem = sraAttributesKey.get(attkey);
            if(attKeyItem == null) {
                attKeyItem = createItem("AttributeKey");

                attKeyItem.setAttribute("name",attkey);
                sraAttributesKey.put(attkey,attKeyItem);
                store(attKeyItem);
            }
            Item attValueItem = createItem("AttributeValue");
            attValueItem.setAttribute("value", attsMap.get(attkey));
            store(attValueItem);

            Item attKeyValueItem = createItem("SRAAttribute");
            attKeyValueItem.setReference("attributeName",attKeyItem);
            attKeyValueItem.setReference("attributeValue",attValueItem);

            //attKeyValueItem.setReference("sraObject",expItem);

            store(attKeyValueItem);
            detialsCollection.add(attKeyValueItem.getIdentifier());
        }

        // lookup for lIbrary name
        if(attsMap.containsKey(EXP_ATT_LIBRARY_NAME)) {
            expItem.setAttributeIfNotNull("libraryName",attsMap.get(EXP_ATT_LIBRARY_NAME));
        }

        expItem.setCollection("experimentDetails",detialsCollection);
    }


    private void addExperimentAttributes(Item expItem, NodeList  expAttTopNodes ) throws ObjectStoreException {

        AttributesMap attributesMap = collectAttributesMap(expAttTopNodes);

        // process extra logic here

        addSRAAttributes(expItem,attributesMap.attsMap,attributesMap.attsUnitMap);



    }

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    private  void processRunsNodes(NodeList runNodes) throws ObjectStoreException {
        for(int i =0; i <  runNodes.getLength();i++ ) {
            processRun(runNodes.item(i));
        }
    }



    private  void processRun(Node runNode) throws ObjectStoreException {
        // [cluster_name, static_data_available, total_spots, broker_name, xmlns:xsi, accession, published, xmlns, center_name, size, run_center, load_done, is_public, assembly, alias, total_bases]

        String sraRunAcc = runNode.getAttributes().getNamedItem("accession").getNodeValue();


        // each run should have an EXPERIMENT_REF, just keep track of the ref accession number and refId of the store Experiment

        Item sraRunItem = sraObjects.get(sraRunAcc);

        if(sraRunItem == null) {
            sraRunItem = createItem("SRARun");
            sraObjects.put(sraRunAcc,sraRunItem);
            // currently adding runs for
            sraRunItem.setReference("sraExperiment", currentSRAExperiment);


            // set main accession id
            sraRunItem.setAttribute("accession", sraRunAcc);

            // if has alias set it
            if (runNode.getAttributes().getNamedItem("alias") != null) {
                String alias = runNode.getAttributes().getNamedItem("alias").getNodeValue();
                sraRunItem.setAttribute("alias", alias);

            }


            for (int j = 0; j < runNode.getChildNodes().getLength(); j++) {
                Node childNode = runNode.getChildNodes().item(j);


                if ("TITLE".equals(childNode.getNodeName())) {
                    String title = getInnerText(childNode);
                    System.out.println("title:" + title);
                    if (title != null)
                        sraRunItem.setAttribute("title", title);
                }




           /* if("EXPERIMENT_REF".equals(childNode.getNodeName())) {
                String experimentRefAcc = getExperimentRefAccession(childNode);
                //Item experimentItem = getExperimentItem(experimentRefAcc);
            }*/
            }


            store(sraRunItem);
        }
    }

    private Item getExperimentItem(String experimentAcc) {
        if(sraObjects.containsKey(experimentAcc)) {
            return  sraObjects.get(experimentAcc);
        }
        Item newExpr = createItem("SRAExperiment");
        newExpr.setAttribute("accession",experimentAcc);
        sraObjects.put(experimentAcc,newExpr);
        return  newExpr;
    }

    private Item getStudyItem(String studyAcc) {
        if(sraObjects.containsKey(studyAcc)) {
            return  sraObjects.get(studyAcc);
        }
        Item newStudy = createItem("SRAStudy");
        newStudy.setAttribute("accession",studyAcc);
        sraObjects.put(studyAcc,newStudy);
        return  newStudy;
    }
    private Item getSampleItem(String sampleAcc) {
        if(sraObjects.containsKey(sampleAcc)) {
            return  sraObjects.get(sampleAcc);
        }
        Item newSample = createItem("SRASample");
        newSample.setAttribute("accession",sampleAcc);
        sraObjects.put(sampleAcc,newSample);
        return  newSample;
    }

    static String getInnerText(Node node) throws IllegalArgumentException {
        Node textNode = node.getFirstChild();
        if(textNode == null)
            return  null;
        if(!textNode.getNodeName().equals("#text"))
            throw  new IllegalArgumentException("Input node is not textNode");
        return textNode.getNodeValue();
    }

    /*private static String getExperimentRefAccession(Node childNode) {
        return  childNode.getAttributes().getNamedItem("accession").getNodeValue();
    }*/

    HashMap<String,String> pubs = new HashMap<>();

    /**
     * add publication and return ref id
     * @param pubMedId
     * @return
     * @throws ObjectStoreException
     */
    private String getPub(String pubMedId) throws ObjectStoreException {
        String refId = pubs.get(pubMedId);

        if (refId == null) {
            Item item = createItem("Publication");
            item.setAttribute("pubMedId", pubMedId);
            pubs.put(pubMedId, item.getIdentifier());
            store(item);
            refId = item.getIdentifier();
        }

        return refId;
    }

}
