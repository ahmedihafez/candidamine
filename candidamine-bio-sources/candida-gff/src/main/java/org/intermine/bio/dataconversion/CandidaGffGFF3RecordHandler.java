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

        if ("Gene".equals(clsName)) {
            // move Gene.primaryIdentifier to Gene.secondaryIdentifier
            // and remove Gene.primaryIdentifier

            //
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


                feature.setAttribute("secondaryIdentifier", secondary);
                feature.removeAttribute("primaryIdentifier");
                if(!StringUtils.isEmpty(primaryIdentifier))
                    feature.setAttribute("primaryIdentifier", primaryIdentifier);

            }

            String secondary = feature.getAttribute("secondaryIdentifier").getValue();
            String symbol = feature.getAttribute("symbol").getValue();
            // System.err.println("Gene secondary " + secondary);
            // System.err.println("Gene symbol " + symbol);
            // clear duplicate names and symbol if it is the same
            if(secondary.equals(symbol)) {
                feature.removeAttribute("symbol");
            }
        }
    }

    @Override
    public void addSynonyms(List<String> synonyms) {
       //this.synonyms.addAll(synonyms);
        // do nothing here :: Maybe a a bug in  base handler
    }

}
