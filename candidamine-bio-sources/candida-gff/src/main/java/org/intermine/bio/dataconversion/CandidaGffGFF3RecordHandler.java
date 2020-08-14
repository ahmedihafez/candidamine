package org.intermine.bio.dataconversion;
import org.apache.commons.lang.StringUtils;

/*
 * Copyright (C) 2002-2019 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

/**
 * A converter/retriever for the CandidaGff dataset via GFF files.
 */

public class CandidaGffGFF3RecordHandler extends GFF3RecordHandler
{
    protected IdResolver rslv;
    /**
     * Create a new CandidaGffGFF3RecordHandler for the given data model.
     * @param model the model for which items will be created
     */
    public CandidaGffGFF3RecordHandler (Model model) {
        super(model);
        // TODO :: revise
        refsAndCollections.put("Exon", "transcripts");
        refsAndCollections.put("MRNA", "gene");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        if (rslv == null) {
            rslv = IdResolverService.getCgdIdResolver();
        }
        // This method is called for every line of GFF3 file(s) being read.  Features and their
        // locations are already created but not stored so you can make changes here.  Attributes
        // are from the last column of the file are available in a map with the attribute name as
        // the key.   For example:
        //
        //     Item feature = getFeature();
        //     String symbol = record.getAttributes().get("symbol");
        //     feature.setAttribute("symbol", symbol);
        //
        // Any new Items created can be stored by calling addItem().  For example:
        // 
        //     String geneIdentifier = record.getAttributes().get("gene");
        //     gene = createItem("Gene");
        //     gene.setAttribute("primaryIdentifier", geneIdentifier);
        //     addItem(gene);
        //
        // You should make sure that new Items you create are unique, i.e. by storing in a map by
        // some identifier. 
        Item feature = getFeature();
        String clsName = feature.getClassName();

        // Bug :: some features are not of type gene
        // so resolving should be in general regardless of the type
        // if we have a an id for this feature in our ids then move/swap


        // No need for apply this to only genes



        if (feature.getAttribute("primaryIdentifier") != null) {
            String secondary = feature.getAttribute("primaryIdentifier").getValue();
            String primaryIdentifier =  null;
            String taxonId = getOrganism().getAttribute("taxonId").getValue();
            if (rslv!= null && rslv.hasTaxon(taxonId)) {
                int resCount = rslv.countResolutions(taxonId, "gene", secondary);
                if (resCount == 1) {
                    primaryIdentifier = rslv.resolveId(taxonId, "gene", secondary).iterator().next();
                }

            }
            // if resoved then swap otherwise leave as it is
            if(primaryIdentifier != null) {
                feature.setAttribute("secondaryIdentifier", secondary);
                feature.removeAttribute("primaryIdentifier");
                if(!StringUtils.isEmpty(primaryIdentifier))
                    feature.setAttribute("primaryIdentifier", primaryIdentifier);
            }



        }

        // if symbol is same as secondaryIdentifier the clean it.
        if(feature.hasAttribute("secondaryIdentifier")) {
            String secondary = feature.getAttribute("secondaryIdentifier").getValue();
            if (feature.hasAttribute("symbol")) {
                String symbol = feature.getAttribute("symbol").getValue();
                // System.err.println("Gene symbol " + symbol);
                // clear duplicate names and symbol if it is the same
                if (secondary.equals(symbol)) {
                    feature.removeAttribute("symbol");
                }
            }
        }
        if ("Gene".equals(clsName)) {
            // move Gene.primaryIdentifier to Gene.secondaryIdentifier
            // and remove Gene.primaryIdentifier

            //



        }

        // Problem :: CDS with same ids ??
        // GFF file from cgd has cds with the same ids :: How to resolve ?
        // In this configuration CDS correspond to one exon
        // TOSOLVE :: cache exons by location information
        // if a CDS has same coordinate then rename the id with the same exon order

        if( "CDS".equals(clsName)) {
            if ( record.getAttributes().get("Parent") != null )
            {
                String transcriptId = record.getAttributes().get("Parent").get(0);

                if(!cdsCountPerTranscript.containsKey(transcriptId)) {
                    cdsCountPerTranscript.put(transcriptId,0);
                }

                cdsCountPerTranscript.put(transcriptId,cdsCountPerTranscript.get(transcriptId)+1);
                int counter = cdsCountPerTranscript.get(transcriptId);


                String primaryIdentifier = feature.getAttribute("primaryIdentifier").getValue();
                primaryIdentifier = primaryIdentifier + counter;
                feature.setAttribute("primaryIdentifier", primaryIdentifier);
            }
            String primaryIdentifier = feature.getAttribute("primaryIdentifier").getValue();
            System.err.println("Getting CDS " + primaryIdentifier);

        }
    }

    // cache features to child
    HashMap<String,Integer> cdsCountPerTranscript = new HashMap<>();
    @Override
    public void addSynonyms(List<String> synonyms) {
       //this.synonyms.addAll(synonyms);
        // do nothing here :: Maybe a a bug in  base handler
    }

}
