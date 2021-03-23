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

import java.io.Reader;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;


import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * 
 * @author
 */
public class CandidaRnaConverter extends BioFileConverter
{

    protected static final Logger LOG = Logger.getLogger(CandidaRnaConverter.class);
    //
    private  String dataSetTitle = null;
    private  String dataSourceName = null;
    private String dataSet = null;

    private String taxonId  = null;

    private  String fileEnding  = null;

    protected IdResolver rslv;


    HashMap<String,Item> rnaResultsItems = new HashMap<>();

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

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public CandidaRnaConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // create data set
        if(dataSet == null)
        {
            setupDataset();
        }
        if (rslv == null) {
            rslv = IdResolverService.getCgdIdResolver();
        }



        File currentFile = getCurrentFile();
        Pattern filePattern = Pattern.compile("^(\\S+)"+fileEnding);
        Matcher matcher = filePattern.matcher(currentFile.getName());
        if (matcher.find()) {
            String sraRunAcc = matcher.group(1);
            System.err.println("Loading data from sraRunAcc  " + sraRunAcc + ", From File " + currentFile.getName());
            // String bioSampleRefId = getBioSample(sraRunAcc);
            // Create BioSample
            // TODO :: load info about it
            // TODO :: cache SRARun incase we have more than one
            Item srarunItem = createItem("SRARun");
            srarunItem.setAttribute("accession",sraRunAcc);


            Item rnaSeqResultSet = createItem("RNASeqResultSet");
            rnaSeqResultSet.setReference("sraRun", srarunItem);

            String rnaseqResultRef = rnaSeqResultSet.getIdentifier();

            rnaResultsItems.put(sraRunAcc,rnaSeqResultSet);

            // should i store it before ??
            store(srarunItem);
            //store(rnaSeqResultSet);

            Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
            // Name	Length	EffectiveLength	TPM	NumReads
            // skip header :: TODO :: check first
            String[] lineTokens = (String[]) lineIter.next();
            while (lineIter.hasNext()) {
                lineTokens = (String[]) lineIter.next();
                String geneIdentifier = lineTokens[0];
                String effectiveLength = lineTokens[2];
                String tpm = lineTokens[3];
                String numReads = lineTokens[4];

                String geneRef = getGene(geneIdentifier);


                Item bioSampleRead = createItem("RNASeqResult");
                bioSampleRead.setReference("gene", geneRef);
                bioSampleRead.setReference("rnaseqResultSet", rnaseqResultRef);

                try {
                    float effectiveLengthFloat = Float.parseFloat(effectiveLength);
                    bioSampleRead.setAttribute("effectiveLength",String.valueOf(effectiveLengthFloat));
                }
                catch (Exception ex ) {
                    throw new Exception("effectiveLength is not well formated (" + effectiveLength + ") in biosample "+ sraRunAcc + " in gene " + geneIdentifier );
                }

                //bioSampleRead.setAttribute("tpm",tpm);
                try {
                    float tpmFloat = Float.parseFloat(tpm);
                    bioSampleRead.setAttribute("tpm",String.valueOf(tpmFloat));
                }
                catch (Exception ex ) {
                    throw new Exception("tpm is not well formated (" + tpm + ") in biosample "+ sraRunAcc + " in gene " + geneIdentifier  );
                }

                try {
                    float numReadsFloat = Float.parseFloat(numReads);
                    bioSampleRead.setAttribute("numReads",String.valueOf(numReadsFloat));
                }
                catch (Exception ex ) {
                    throw new Exception("numReads is not well formated (" + numReads + ") in biosample "+ sraRunAcc + " in gene " + geneIdentifier );
                }

                store(bioSampleRead);

            }
        }
        else {
            System.err.println("Loading data from " + currentFile.getName());
            if(currentFile.getName().contains("strand_info")) {

                // process strand_info.txt files
                    Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
                    // sample	mapping_rate	strandedness	note
                    // skip header :: TODO :: check first
                    String[] lineTokens = (String[]) lineIter.next();
                    while (lineIter.hasNext()) {
                        lineTokens = (String[]) lineIter.next();
                        String sample = lineTokens[0];
                        String mapping_rate = lineTokens[1].replace("%","");
                        String strandedness = lineTokens[2];
                        String note = lineTokens[3];
                        if(rnaResultsItems.containsKey(sample)) {
                            Item rnaResultItem = rnaResultsItems.get(sample);
                            rnaResultItem.setAttribute("mappingRate",mapping_rate);
                            rnaResultItem.setAttribute("strandedness",strandedness);
                            rnaResultItem.setAttribute("note",note);
                            store(rnaResultItem);
                        }
                    }
                }
        }
    }

    protected String getBioSample(String bioSampleName) {
        return bioSampleName;
    }

    private Map<String, String> identifiersToGenes = new HashMap<String, String>();
    private String getGene(String geneId)
            throws ObjectStoreException {
        boolean resolved = false;
        // For now use the id privided it is the primary id used in the database
        String resolvedIdentifier = geneId;
        // if rslv exit and has taxond of this organims try to resolve the primary id.
        // If not then use the provided id in the file as primary
        if (rslv!= null &&  taxonId != null && rslv.hasTaxon(taxonId)  ) {
            int resCount = rslv.countResolutions(taxonId, "gene", geneId);
            if (resCount == 1) {
                resolvedIdentifier = rslv.resolveId(taxonId, "gene", geneId).iterator().next();
                resolved = true;
            }
            else if(resCount == 0) {
                System.err.println("Can not resolve Gene " + geneId + ". Will use given id instead");
            }
            else {
                // can not resolve the id
                // this mean that this gene might no exit in the current version or no infomation is provided about it so do not add
                // resolvedIdentifier = null;
                return null;
            }
        }




        String refId = identifiersToGenes.get(resolvedIdentifier);

        if (refId == null  ) {

            if (!StringUtils.isEmpty(resolvedIdentifier)) {
                Item item = createItem("Gene");
                if(resolved)
                    item.setAttribute("primaryIdentifier", resolvedIdentifier);
                else
                    item.setAttribute("secondaryIdentifier", resolvedIdentifier);

                refId = item.getIdentifier();
                identifiersToGenes.put(resolvedIdentifier, refId);
                store(item);
            }


        }
        return refId;
    }
}
