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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.util.FormattedTextParser;
import org.apache.commons.lang.StringUtils;
import org.intermine.objectstore.ObjectStoreException;


/**
 * 
 * @author
 */
public class CgdIprScanConverter extends BioFileConverter
{
    //
    private  String dataSetTitle = null;
    private  String dataSourceName = null;
    private String dataSet = null;



    private Map<String, String> identifiersToGenes = new HashMap<String, String>();
    private Map<String, String> identifiersToDomain = new HashMap<String, String>();
    private Map<String, String>  identifiersToFeatures= new HashMap<String, String>();

    protected IdResolver rslv;


    private String taxonId  = null;

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
    public CgdIprScanConverter(ItemWriter writer, Model model) {
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

        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        while (lineIter.hasNext()) {
//        	Columns:
//    		    		1.  Systematic identifier of the input sequence
//    		    		2.  crc64 (unique checksum) of the sequence
//    		    		3.  Length of the sequence (in amino acids)
//    		    		4.  Analysis method
//    		    		5.  Source database entry for this match
//    		    		6.  Source database description for the entry
//    		    		7.  Start coordinate of the domain match
//    		    		8.  End coordinate of the domain match
//    		    		9.  E-value of the match (reported by analysis method)
//    		    		10. Status of the match (T: true; ?: unknown)
//    		    		11. Date of the IprScan run
//    		    		12. InterPro domain identifier
//    		    		13. InterPro domain description
//    		    		14. GO (gene ontology) description for the InterPro domain
            String[] lineTokens = (String[]) lineIter.next();

            String identifier = lineTokens[0];
            String analysisMethod = lineTokens[3];
            String sourceDBId = lineTokens[4];
            String desc = lineTokens[5];
            String start = lineTokens[6];
            String end = lineTokens[7];
            String matchStatus =  lineTokens[9];
            String domainIdentifier = lineTokens[11];


            if(StringUtils.isEmpty(domainIdentifier) || "NULL".equals(domainIdentifier))
            {
                // Skip this record
                continue;
            }

            if(StringUtils.isEmpty(matchStatus) || "?".equals(matchStatus))
            {
                // Skip  record  with unknown status
                continue;
            }
            // get gene
            // get transcript in  the gene
            //
            String geneRef = getGene(identifier);
            if(StringUtils.isEmpty(geneRef))
            {
                // Skip this record
                continue;
            }


            String domainRef = getDomain(domainIdentifier);
            // this would be MRNA
//    		String parentFeatureRef = getFeature(identifier);



            // create new record
            Item  newRecord =  createItem("PolypeptideDomain");
            newRecord.setAttribute("secondaryIdentifier", identifier+"_"+domainIdentifier);
            // newRecord.setAttribute("start", value);
            newRecord.setReference("gene", geneRef);
            newRecord.setReference("proteinDomain", domainRef);
            newRecord.setAttribute("transcriptStart", String.valueOf(start));
            newRecord.setAttribute("transcriptEnd", String.valueOf(end));
            newRecord.setAttribute("transcriptId",identifier+"-T");
//    		Item location = createItem("Location");
//        	location.setAttribute("start", String.valueOf(start));
//        	location.setAttribute("end", String.valueOf(end));
//            // location.setAttribute("strand", "0");
//            location.setReference("locatedOn", parentFeatureRef);
//            location.setReference("feature", newRecord.getIdentifier());
//            store(location);

            if(!StringUtils.isEmpty(desc) && !"no description".equals(desc)) {
                newRecord.setAttribute("description",desc);
            }
            if(!StringUtils.isEmpty(sourceDBId) ) {
                newRecord.setAttribute("primaryIdentifier",sourceDBId);
            }
            if(!StringUtils.isEmpty(analysisMethod) ) {
                newRecord.setAttribute("analysisMethod",analysisMethod);
            }

            store(newRecord);

        }




    }




    private String getDomain(String domainIdentifier)
            throws ObjectStoreException {
        String refId = identifiersToDomain.get(domainIdentifier);
        if (refId == null  ) {
            Item  newDomain =  createItem("ProteinDomain");
            newDomain.setAttribute("primaryIdentifier", domainIdentifier);
            refId = newDomain.getIdentifier();
            identifiersToDomain.put(domainIdentifier, refId);
            store(newDomain);
        }
        return refId;
    }
    private String getFeature(String identifier)
            throws ObjectStoreException {
        String featureIdentifier = identifier + "-T" ;

        String refId = identifiersToFeatures.get(featureIdentifier);

        String featureType = "MRNA";


        if (refId == null  ) {
            Item  newFeature =  createItem("MRNA");
            newFeature.setAttribute("primaryIdentifier", featureIdentifier);
            refId = newFeature.getIdentifier();
            identifiersToFeatures.put(featureIdentifier, refId);
            store(newFeature);
        }
        return refId;
    }




    private String getGene(String geneId)
            throws ObjectStoreException {
        // For now use the id privided it is the primary id used in the database
        String resolvedIdentifier = geneId;
        // if rslv exit and has taxond of this organims try to resolve the primary id.
        // If not then use the provided id in the file as primary
        if (rslv!= null &&  taxonId != null && rslv.hasTaxon(taxonId)  ) {
            int resCount = rslv.countResolutions(taxonId, "gene", geneId);
            if (resCount == 1) {
                resolvedIdentifier = rslv.resolveId(taxonId, "gene", geneId).iterator().next();
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
                item.setAttribute("primaryIdentifier", resolvedIdentifier);
                refId = item.getIdentifier();
                identifiersToGenes.put(resolvedIdentifier, refId);
                store(item);
            }


        }
        return refId;
    }
}
