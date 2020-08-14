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
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class StringDbConverter extends BioFileConverter
{

    private  String fileEnding  = "intc";





    protected IdResolver rslv;


    private String taxonId  = null;



    public void setTaxonId(String taxonId)
    {
        this.taxonId = taxonId;
    }




    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public StringDbConverter(ItemWriter writer, Model model) {
        super(writer, model, "STRING's", "STRING's interaction data set",
                "https://creativecommons.org/licenses/by/4.0/");
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // create data set

        if (rslv == null) {
            rslv = IdResolverService.getCgdIdResolver();
        }

        File currentFile = getCurrentFile();
        Pattern filePattern = Pattern.compile("^(\\S+)"+fileEnding);
        Matcher matcher = filePattern.matcher(currentFile.getName());
        if (matcher.find()) {
            String taxonId = matcher.group(1);
            System.err.println("Loading data from taxonId  " + taxonId + ", From File " + currentFile.getName());

            Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
            // protein1 protein2 neighborhood fusion cooccurence coexpression experimental database textmining combined_score
            // skip header :: TODO :: check first
            String[] lineTokens = (String[]) lineIter.next();
            while (lineIter.hasNext()) {
                lineTokens = (String[]) lineIter.next();
                String gene1Identifier = lineTokens[0];
                String gene2Identifier = lineTokens[1];
                String neighborhood= lineTokens[2];
                String fusion= lineTokens[3];
                String cooccurence= lineTokens[4];
                String coexpression= lineTokens[5];
                String experimental= lineTokens[6];
                String database= lineTokens[7];
                String textmining= lineTokens[8];
                String combined_score= lineTokens[9];

                String gene1Ref = getGene(gene1Identifier);
                String gene2Ref = getGene(gene2Identifier);
                Item interactionItem = createItem("Interaction");
                interactionItem.setReference("participant1",gene1Ref);
                interactionItem.setReference("participant2",gene2Ref);
                store(interactionItem);

                Item detail = createItem("StringInteractionDetail");
                detail.setAttribute("role1", "gene");
                detail.setAttribute("role2", "gene");
                detail.setAttribute("relationshipType", "Inferred interaction");
                detail.setAttribute("type", "genetic");
                String prettyName = gene1Identifier +  "_"+gene2Identifier;
                detail.setAttribute("name", "String:" + prettyName);
                detail.setReference("interaction", interactionItem);


                detail.setAttribute("neighborhoodScore", lineTokens[2]);
                detail.setAttribute("fusionScore", lineTokens[3]);
                detail.setAttribute("cooccurenceScore", lineTokens[4]);
                detail.setAttribute("coexpressionScore", lineTokens[5]);
                detail.setAttribute("experimentalScore", lineTokens[6]);
                detail.setAttribute("databaseScore", lineTokens[7]);
                detail.setAttribute("textminingScore", lineTokens[8]);
                detail.setAttribute("combinedScore", lineTokens[9]);

                store(detail);


            }


        }


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
                item.setAttribute("primaryIdentifier", resolvedIdentifier);
                refId = item.getIdentifier();
                identifiersToGenes.put(resolvedIdentifier, refId);
                store(item);
            }


        }
        return refId;
    }
}
