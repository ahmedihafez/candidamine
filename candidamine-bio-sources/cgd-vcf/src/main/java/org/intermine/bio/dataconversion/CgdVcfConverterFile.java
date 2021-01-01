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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;


/**
 * 
 * @author
 */
public class CgdVcfConverterFile extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(CgdVcfConverterFile.class);

    // keep list here
    private final Map<String, String> dataSets = new HashMap<String, String>();
    // For testing
    private int MAX_LINES_TEST = 1000;
    private int currentLine = 0;


    private String dataSet = null;

    //
    private  String dataSetTitle = null;
    private  String dataSourceName = null;
    private  String dataSetDesc = null;


    private String taxonId  = null;
    private String pubmedIds= null;
    protected IdResolver rslv;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public CgdVcfConverterFile(ItemWriter writer, Model model) {
        super(writer, model);
        // static filters
        passFilters.add("PASS");
    }


    public void setDataSet(String dataSetTitle)
    {
        this.dataSetTitle = dataSetTitle;
    }
    public void setDataSource(String dataSource)
    {
        this.dataSourceName = dataSource;
    }
    public void setDataSetDesc(String dataSetDesc)
    {
        this.dataSetDesc = dataSetDesc;
    }


    public void setTaxonId(String taxonId)
    {
        this.taxonId = taxonId;
    }
    public void setPubmedIds(String pubmedIds)
    {
        this.pubmedIds = pubmedIds;
    }

    // chromosomes seq
    private Map<String, Item> seqs = new HashMap<String, Item>();
    //
    private Map<Integer, Item > strains =  new HashMap<Integer, Item>();
    private Map<String, Item > snpItems =  new HashMap<String, Item>();
    private List<String> passFilters = new ArrayList<String>();
    private String organismRefId =  null;
    // current active source vcf file
    Item varationSource;





    /**
     * Return a DataSet ref with the given details.
     *
     * @param title the DataSet title
     * @param dataSourceRefId the DataSource referenced by the the DataSet
     * @return the DataSet Item
     */
    @Override
    public String getDataSet(String title, String dataSourceRefId) {
        return getDataSet(title,dataSourceRefId,null);
    }

    public String getDataSet(String title, String dataSourceRefId , String desc) {
        String refId = dataSets.get(title);
        if (refId == null) {
            Item dataSet = createItem("VariationSet");
            varationSource = dataSet;
            dataSet.setAttribute("name", title);
            dataSet.setReference("dataSource", dataSourceRefId);
            for(String pubidIdRef : pubs.values()) {
                varationSource.addToCollection("publications",pubidIdRef);
            }
            if (!StringUtils.isEmpty(desc))
                dataSet.setAttribute("description", desc);
            try {
                store(dataSet);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store DataSet with title: " + title, e);
            }
            refId = dataSet.getIdentifier();
            dataSets.put(title, refId);
        }
        return refId;
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





    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        if(pubmedIds != null) {
            // get publication and set it up for later use
            String[] pubmedIdsArray = pubmedIds.split(",");
            if(pubmedIdsArray.length > 0) {
                for (int i = 0 ;i < pubmedIdsArray.length ; i++){
                    // add pubid
                    // this would be for the whole source
                    getPub(pubmedIdsArray[i]);
                }
            }
        }
        // create data set
        if(dataSet == null)
        {
            setupDataset();
        }


        if (rslv == null) {
            rslv = IdResolverService.getCgdIdResolver();
        }


        //  System.out.println("taxonId = " + taxonId);
        organismRefId = getOrganism(taxonId);
        //System.out.println("organismRefId = " + organismRefId);

        File currentFile = getCurrentFile();
        Pattern filePattern = Pattern.compile("^(\\S+).vcf");
        Matcher matcher = filePattern.matcher(currentFile.getName());
        if (matcher.find()) {
            // create source
            System.err.println("Loading data from " + currentFile.getName());

            String sourceFileName = matcher.group(1);

            //varationSource = createItem("VariationSource");
            // varationSource.setAttribute("name",sourceFileName);
            // add publication to it

            //store(varationSource);
            // reset strains
            strains =  new HashMap<Integer, Item>();
            currentLine = 0;
            processVCF(reader);
            // store stains
            for(Item strain : strains.values()) {
                store(strain);
            }
        }





    }

    private void processVCF(Reader reader) throws IOException {
        final CSVReader bufferedReader = new CSVReader(reader, '\t', '"');


        while (true) {
            String[] line = bufferedReader.readNext();
            if (line == null) {
                // EOF
                break;
            }

            // FIXME :: This is for testing
/*            if( currentLine > MAX_LINES_TEST)
                break;*/



            if (line[0].startsWith("##"))
            {
                // Skip it is a comment Line
                currentLine ++;
                continue;
            }

            // if header line process it to git isolates information
            if (line[0].equals("#CHROM"))
            {
                // System.out.println("Header Info");
                // if (true)
                //	throw new RuntimeException("Header Info : " + currentLine);
                processHeader(line);
            }
            else
            {
                // process the record and create snp data
                processRecord(line);
            }
            currentLine ++;
        }
    }

    private void processHeader(String[] line) {

        // start index in the line array for the strains info
        try
        {
/*
            System.err.println("Store Strain data  ...." );
*/

            for (int i = isolates_CLM_Start ; i <  line.length ; i ++ )
            {
                String strainName = line[i];
                Item strain = createItem("Strain");
                strain.setAttribute("primaryIdentifier", strainName);
                strain.setReference("organism",organismRefId);
                strains.put(i, strain);
                // TODO :: do not store strain yet ??
                //store(strain);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("failed to store Isolates at line : " + currentLine, e);

        }
    }


    private void processRecord(String[] line) {

        try {


            // File Format
            // #CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT





            // #CHROM
            String chromosomeIdentifier = line[0];

            // POS
            String start = line[1];
            // ID --
            String identifier = line[2];
            // REF
            String referenceSeq = line[3];
            // ALT
            String variantSeq = line[4];
            // QUAL
            String qualityScore = line[5];
            // FILTER
            String filter =  line[6];

            // INFO
            String info = line[7];

            // Format
            String format = line[8];

            // If bad quality do not add this line
            if( !passFilter( filter)) //
            {
                // skip the feature
                return;
            }


            Map<Integer,List<Item>> snpsStrains = processStrain(line);
            // which strain have the mutation


            // FIXME :: replace seqid with the correct one from the gff file.

            String[] nameParts = chromosomeIdentifier.split("chr");
            // TEMP Fix for the name :: just for Testing
            if(chromosomeIdentifier.contains("mitoc"))
            {
                chromosomeIdentifier = "mito_C_glabrata_CBS138";
            }
            else
            {
                chromosomeIdentifier = "Chr"+nameParts[1] + "_C_glabrata_CBS138";
            }


            // create SNV by default?







            // create chromosme seq
            //	        Item chromosome = getChromosome(chromosomeIdentifier);
            String[] variants = null ;
            if(StringUtils.contains(variantSeq, ","))
            {
                variants = variantSeq.split(",");
            }
            else
            {
                variants = new String[] {variantSeq};
            }
            // Which alt index
            int gtIndex = 1;
            String annPart = getInfoAtr(info,"ANN");

            HashMap<String,ArrayList<Item>> consToVariant= processANN(annPart);
            // System.err.println("line " + currentLine );


            //System.err.println("annPart " + annPart );

            //System.err.println("consToVariant " + consToVariant );
            //System.err.println("variantSeq " + variantSeq );

            for(String variant : variants)
            {
                String type =   getVariantType(referenceSeq,variant);//           "SequenceAlteration";

                String idPrefix = "";
                identifier = idPrefix +  chromosomeIdentifier   + "_" +start + "_"+    referenceSeq + "_" + variant;
                Item snp = snpItems.get(identifier);
                if(snp ==  null ) {
                     snp = createRecord(identifier, start, referenceSeq, variant, chromosomeIdentifier);
                     // snp logic
                     store(snp);
                    snpItems.put(identifier,snp);
                    // intermine dose not add ref in such case where there is no id or key in the object.??
                    for(Item cons : consToVariant.get(variant)) {
                        // do not add to collection
                        // snp.addToCollection("consequences",cons );
                        cons.setReference("sequenceAlteration",snp.getIdentifier());
                        store(cons);
                    }
                }

                for(Item stain : snpsStrains.get(gtIndex) ) {
                    // snp.setCollection("variants", snpsStrains.get(gtIndex));
                    stain.addToCollection("sequenceAlterations",snp);
                }
                gtIndex++;

                //System.err.println("variant " + variant );



            }



        }
        catch(ObjectStoreException e)
        {
            throw new RuntimeException("failed to store SequenceAlteration at line : " + currentLine, e);

        }

    }

    /**
     * process ANN info from SnpEff
     * @param annPart
     */
    private HashMap<String,ArrayList<Item>> processANN(String annPart) throws ObjectStoreException {
        // ANN=
        // C|upstream_gene_variant|MODIFIER|EPA19|EPA19|transcript|CAGL0A00110g|pseudogene||n.-3779A>C|||||578|,
        // G|upstream_gene_variant|MODIFIER|EPA19|EPA19|transcript|CAGL0A00110g|pseudogene||n.-3779A>G|||||578|,
        // C|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0F00110g|pseudogene||n.-3766A>C|||||211|,
        // G|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0F00110g|pseudogene||n.-3766A>G|||||211|,
        // C|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0E00110g|pseudogene||n.-5390A>C|||||410|,
        // G|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0E00110g|pseudogene||n.-5390A>G|||||410|,
        // C|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0J00110g|pseudogene||n.-3121A>C|||||547|,
        // G|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0J00110g|pseudogene||n.-3121A>G|||||547|,
        // C|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0B00110g|pseudogene||n.-3378A>C|||||671|,
        // G|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0B00110g|pseudogene||n.-3378A>G|||||671|,
        // C|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0I00110g|pseudogene||n.-7294A>C|||||1377|,
        // G|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0I00110g|pseudogene||n.-7294A>G|||||1377|,
        // C|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0G00110g|pseudogene||n.-4653A>C|||||1696|,
        // G|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0G00110g|pseudogene||n.-4653A>G|||||1696|,
        // C|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0D00110g|pseudogene||n.-4409A>C|||||2058|,
        // G|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0D00110g|pseudogene||n.-4409A>G|||||2058|,
        // C|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0H00132g|pseudogene||n.-6562A>C|||||3199|,
        // G|upstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0H00132g|pseudogene||n.-6562A>G|||||3199|,
        // C|downstream_gene_variant|MODIFIER|CAGL0A00110g|CAGL0A00110g|transcript|CAGL0A00110g-T|protein_coding||c.*578T>G|||||578|WARNING_TRANSCRIPT_MULTIPLE_STOP_CODONS,
        // G|downstream_gene_variant|MODIFIER|CAGL0A00110g|CAGL0A00110g|transcript|CAGL0A00110g-T|protein_coding||c.*578T>C|||||578|WARNING_TRANSCRIPT_MULTIPLE_STOP_CODONS,
        // C|downstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0M00110g|pseudogene||n.*1028A>C|||||471|,
        // G|downstream_gene_variant|MODIFIER|CAGL0A04873g|null|transcript|CAGL0M00110g|pseudogene||n.*1028A>G|||||471|,
        // C|intragenic_variant|MODIFIER|CAGL0A04873g|null_circ|gene_variant|null_circ|||n.1030A>C||||||,
        // G|intragenic_variant|MODIFIER|CAGL0A04873g|null_circ|gene_variant|null_circ|||n.1030A>G||||||,
        // C|intragenic_variant|MODIFIER|CAGL0A04873g|null|gene_variant|null|||n.1030A>C||||||,
        // G|intragenic_variant|MODIFIER|CAGL0A04873g|null|gene_variant|null|||n.1030A>G||||||

        // consequences
        //System.err.println("annPart " + annPart );
        String[] conss = annPart.split(",");
        HashMap<String,ArrayList<Item>> consToVariant = new HashMap<>();
        for(String cons : conss) {
            System.err.println("cons " + cons );
            // and end ending
            cons = cons + "|END";
            String[] consAtts = cons.split("\\|");
            // Allele | Annotation | Annotation_Impact | Gene_Name | Gene_ID | Feature_Type | Feature_ID | Transcript_BioType | Rank | HGVS.c | HGVS.p | cDNA.pos / cDNA.length | CDS.pos / CDS.length | AA.pos / AA.length | Distance | ERRORS / WARNINGS / INFO' ">
            //   0          1              2                 3          4          5               6           7                  8       9       10        11                            12               13        14         15         16          17
            String allele  = consAtts[0];
            String soTerms = consAtts[1];
            String imapct =  consAtts[2];

            String HGVS_C = consAtts[9];
            String HGVS_P = consAtts[10];

            // HGVS.c: Variant using HGVS notation (DNA level)
            // HGVS.p: If variant is coding, this field describes the variant using HGVS notation (Protein level). Since transcript ID is already mentioned in ‘feature ID’, it may be omitted here.
            // System.err.println("allele " + allele );

            Item consItem = createItem("Consequence");
            consItem.setAttribute("allele",allele);
            consItem.setAttribute("putativeImpact",imapct);
            if(!StringUtils.isEmpty(HGVS_C))
                consItem.setAttribute("nucleotideVariant",HGVS_C);
            if(!StringUtils.isEmpty(HGVS_P))
                consItem.setAttribute("aminoAcidsVariant",HGVS_P);
            addSOTerms(consItem,soTerms);
            //store(consItem);
            if(!consToVariant.containsKey(allele)) {
                consToVariant.put(allele,new ArrayList<Item>());
            }
            consToVariant.get(allele).add(consItem);
        }

       return consToVariant;
    }

    HashMap<String,Item> termRefs = new HashMap<>();
    private void addSOTerms(Item consItem, String soTerms) throws ObjectStoreException {
        String[] terms = soTerms.split("&");
        for(String term : terms) {
            Item termItem = null;
            if(termRefs.containsKey(term)) {
                termItem = termRefs.get(term);
            }
            else {
                termItem = createItem("SOTerm");
                termItem.setAttribute("name",term);
                store(termItem);
                termRefs.put(term,termItem);
            }

            consItem.addToCollection("annotations",termItem);
        }

    }

    /**
     * cut the info part and get attribute with the given key
     * @param info
     * @param ann
     * @return
     */
    private String getInfoAtr(String info, String attKey) {
        String[] atts = info.split(";");
        for (String att: atts) {
            String[] keyValue = att.split("=");
            if(keyValue.length > 1) {
                if(attKey.equals(keyValue[0]))
                    return keyValue[1];
            }

        }
        return  null;
    }

    final int isolates_CLM_Start = 9 ;
    private Map<Integer,List<Item>> processStrain(String[] line) {

        String format = line[8];
        // GT value will be always the first one no need to inspect format Now
        Map<Integer,List<Item>> snps_isolates = new HashMap<Integer,List<Item>>();
        for (int i = isolates_CLM_Start ; i <  line.length ; i ++ )
        {
            String isolateValues = line[i];
            if(!isolateValues.equals("0")) // if not empty just add it to the SNP
            {
                String[] formatValues = isolateValues.split(":");
                int gtIndex = new Integer(formatValues[0]);

                if (!snps_isolates.containsKey(gtIndex))
                {
                    snps_isolates.put(gtIndex, new ArrayList<Item>());
                }

                // String isolateRefId = strains.get(i).getIdentifier();
                // snps_isolates.get(gtIndex).add(isolateRefId);
                snps_isolates.get(gtIndex).add(strains.get(i));
            }
        }
        return snps_isolates;

    }


    private String getVariantType(String referenceSeq, String variantSeq) {
        // TODO :: Just for not create base class
        // Types have to be check to create it correct
        String type = "SequenceAlteration";
        if(referenceSeq.length() == variantSeq.length()  &&  referenceSeq.length() == 1)
            type= "SNV";
//	    if(referenceSeq.length() != variantSeq.length()  &&  referenceSeq.length() > 1 &&  variantSeq.length() > 1)
//	    	type= "Substitution";
        if(referenceSeq.length() > variantSeq.length() &&  variantSeq.length() == 1)
            type= "Deletion";
        if(referenceSeq.length() < variantSeq.length() &&  referenceSeq.length() == 1)
            type= "Insertion";

        return type;
    }
    private boolean passFilter(String filter)
    {
        String[] recordFilters = filter.split(Pattern.quote(";"));
        for(String recordFilter : recordFilters)
        {
            if(passFilters.contains(recordFilter))
                return true;
        }
        return false;
    }


    private Item createRecord(String identifier, String start,String referenceSeq,String variantSeq , String chromosomeIdentifier)
            throws ObjectStoreException {



        String type = getVariantType(referenceSeq,variantSeq);






        Item chromosome = getChromosome(chromosomeIdentifier);
        Item snp =  createItem(type);
        snp.setAttribute("primaryIdentifier", identifier);
        snp.setAttribute("referenceSequence", referenceSeq);
        snp.setAttribute("variantSequence", variantSeq);
        snp.setAttribute("type", type);



        int length = referenceSeq.length();
        int startPos = new Integer(start);

        int endPos = startPos + length - 1;
        Item location = getLocation(chromosome,snp,startPos,endPos);

        snp.setReference("chromosome", chromosome);
        snp.setReference("chromosomeLocation",location);
        snp.setReference("organism", organismRefId);
        snp.setReference("variationSource", varationSource);
        return snp;
    }




    private Item getChromosome(String chromosomeIdentifier)
            throws ObjectStoreException {


        String identifier = chromosomeIdentifier;



        Item seq = seqs.get(identifier);
        if (seq == null) {
            seq = createItem("Chromosome");
            seq.setAttribute("primaryIdentifier", identifier);
            seq.setReference("organism", organismRefId);
            store(seq);
            seqs.put(identifier, seq);
        }
        return seq;
    }

    private Item getLocation(Item seq ,Item feature, int start , int end)
            throws ObjectStoreException {
        Item location = createItem("Location");
        location.setAttribute("start", String.valueOf(start));
        location.setAttribute("end", String.valueOf(end));
        location.setAttribute("strand", "0");
        location.setReference("locatedOn", seq.getIdentifier());
        location.setReference("feature", feature.getIdentifier());

        // TODO :: add ref to the dataset
        //location.addToCollection("dataSets", dataSet);

        store(location);


        return location;

    }




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
