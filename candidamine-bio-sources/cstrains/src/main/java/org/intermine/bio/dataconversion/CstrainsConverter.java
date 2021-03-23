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
public class CstrainsConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(CstrainsConverter.class);

    //
    private static final String DATASET_TITLE = "Candida Strains Collection";
    private static final String DATA_SOURCE_NAME = "CandidaMine";
    private Map<String,String > pmidRefIds = new HashMap<String,String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public CstrainsConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        File currentFile = getCurrentFile();
        Pattern filePattern = Pattern.compile("^(\\S+).strains");
        Matcher matcher = filePattern.matcher(currentFile.getName());

        if (matcher.find()) {
            String organismTaxonId = matcher.group(1);
            LOG.info("Loading data from Organims with taxonId " + organismTaxonId + ", From File " + currentFile.getName());
            String organismRefId = getOrganism(organismTaxonId);
            Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
            while (lineIter.hasNext()) {
                String[] lineTokens = (String[]) lineIter.next();


                // ##Name StrainName	TaxonId	Synonymous ID	PubmedId

                // Isolate and Strain Info
                String strainId = lineTokens[0];
                String strainFullName = lineTokens[1];
                String strainTaxonId = lineTokens[2];
                String synonymousIds = lineTokens[3];
                String hostTaxonId = lineTokens[4];
                String pubmedId = lineTokens[5];

            }
        }
    }
}
