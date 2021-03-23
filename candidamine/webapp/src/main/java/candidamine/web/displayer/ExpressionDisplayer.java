package org.candidamine.web.displayer;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.results.InlineResultsTable;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONArray;


public class ExpressionDisplayer extends ReportDisplayer
{
    private static final Logger LOG = Logger.getLogger(ExpressionDisplayer.class);


//    List<String> runAcc = new ArrayList<String>();
//    List<String> sampleAcc = new ArrayList<String>();
//    List<String> experimentAcc = new ArrayList<String>();
//    List<String> studyAcc = new ArrayList<String>();
//    List<Float> tpm = new ArrayList<Float>();
//    List<Float> numReads = new ArrayList<Float>();
//
//    List<String> objectIds = new ArrayList<String>();

    /**
     * @param config configuration object
     * @param im intermine API
     */
    public ExpressionDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }


    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        System.out.println("In ExpressionDisplayer");
        InterMineObject object = reportObject.getObject();
        request.setAttribute("experssionObjectId", object.getId());
    }


    public void displayOld(HttpServletRequest request, ReportObject reportObject) {
        System.out.println("In ExpressionDisplayer");




/*        for (MicroArrayResult mar: gene.getMicroArrayResults()) {
            if (mar instanceof FlyAtlasResult) {
                FlyAtlasResult far = (FlyAtlasResult) mar;
                objectIds.add(String.valueOf(far.getId()));
                signals.add(far.getMRNASignal());
                names.add(far.getTissue().getName());
                affyCalls.add(far.getAffyCall());
                enrichments.add(far.getEnrichment());
                presentCalls.add(far.getPresentCall());
            }
        }*/


        // get the gene/protein in question from the request
//        InterMineObject object = reportObject.getObject();
//
//        // API connection
//        HttpSession session = request.getSession();
//        im = SessionMethods.getInterMineAPI(session);
//        Model model = im.getModel();
//        PathQuery query = new PathQuery(model);
//
//        // cast me Gene
//        Gene gene = (Gene) object;
//        Object id = gene.getId();
//        System.out.println("In ExpressionDisplayer" + id);
//
//        if (id != null) {
//            // fetch the expression
//            String geneId = String.valueOf(id);
//
//             try {
//                query = getQuery(geneId, query);
//
//                // execute the query
//
//                 Profile profile = SessionMethods.getProfile(session);
//
//                 PathQueryExecutor executor = im.getPathQueryExecutor(profile);
//
//                 ExportResultsIterator values = executor.execute(query);
//
//                 processResults(values);
//                //List results = processResults2(values);
//                 //request.setAttribute("expressionResults", results);
//                 //request.setAttribute("expressionResultsJson", new JSONArray(results));
//
//                // inline collection table to toggle
//                //InlineResultsTable table = processTable(request, reportObject);
//                //request.setAttribute("expressionCollection", table);
//            } catch (Exception e) {
//                //request.setAttribute("expressionResults", new LinkedHashMap<String, Float>());
//                // throw  new Exception("Exception in expressionResults");
//                e.printStackTrace();
//             }
//        }


//
//        request.setAttribute("runAcc", new JSONArray(runAcc.toString()));
//        request.setAttribute("experimentAcc", new JSONArray(experimentAcc.toString()));
//        request.setAttribute("sampleAcc", new JSONArray(sampleAcc.toString()));
//        request.setAttribute("studyAcc", new JSONArray(studyAcc.toString()));
//        request.setAttribute("numReads", numReads.toString());
//        request.setAttribute("tpm", tpm.toString());
//
//       // request.setAttribute("names", new JSONArray(names));
//        //request.setAttribute("affyCalls", new JSONArray(affyCalls));
//        //request.setAttribute("enrichments", enrichments.toString());
//        //request.setAttribute("presentCalls", presentCalls.toString());
//        request.setAttribute("objectIds", new JSONArray(objectIds));
//        System.out.println("Out of ExpressionDisplayer");
    }



//    private  void processResults(ExportResultsIterator it) {
//        Map<String, Float> results = new LinkedHashMap<String, Float>();
//        while (it.hasNext()) {
//            List<ResultElement> row = it.next();
//            String acc =  (String) row.get(0).getField();
//            runAcc.add(acc);
//
//
//            acc =  (String) row.get(4).getField();
//            experimentAcc.add(acc);
//
//            acc =  (String) row.get(5).getField();
//            sampleAcc.add(acc);
//
//            acc =  (String) row.get(6).getField();
//            studyAcc.add(acc);
//
//            Float value =  (Float) row.get(1).getField();
//            tpm.add(value);
//
//            value =  (Float) row.get(3).getField();
//            numReads.add(value);
//
//
//
//
//        }
//    }

    private static PathQuery getQuery(String geneId, PathQuery query) {
        query.addViews(
                "Gene.rnaseqResultReads.rnaseqResult.sraRun.accession",
                "Gene.rnaseqResultReads.tpm",
                "Gene.rnaseqResultReads.effectiveLength",
                "Gene.rnaseqResultReads.numReads",
                "Gene.rnaseqResultReads.rnaseqResult.sraRun.sraExperiment.accession",
                "Gene.rnaseqResultReads.rnaseqResult.sraRun.sraExperiment.sraSample.accession",
                "Gene.rnaseqResultReads.rnaseqResult.sraRun.sraExperiment.sraStudy.accession",
                "Gene.primaryIdentifier");
        query.addConstraint(Constraints.eq("Gene.id", geneId));
        query.addOrderBy("Gene.rnaseqResultReads.rnaseqResult.sraRun.accession", OrderDirection.ASC);
        return query;
    }

}
