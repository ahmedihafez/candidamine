<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- dataCategories -->

<c:set var="note1" value="Only genes that have been mapped to the genome have been loaded"/>

<html:xhtml/>

<div class="body">
<im:boxarea title="Data" stylename="plainbox"><p><fmt:message key="dataCategories.intro"/></p></im:boxarea>


<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  <tr>
    <th>Data Category</th>
    <th>Organism</th>
    <th>Data</th>
    <th>Source</th>
    <th>PubMed</th>
    <th>Note</th>
  </tr>
  <tr>
    <td rowspan="2" class="leftcol"><h2><p>Genomes</p></h2></td>
    <td>
        <p><i>C. albicans</i></p>
        <p><i>C. glabrata</i></p>
        <p><i>C. parapsilosis</i></p>
        <p><i>C. tropicalis</i></p>
    </td>
    <td>Genomic sequences and  annotations</td>
    <td><a href="http://www.candidagenome.org/" target="_new" class="extlink">Candida Genome Database (CGD)</a>  (June,2019 updates)  </td>
    <td>  <a href="https://pubmed.ncbi.nlm.nih.gov/27738138/" target="_new" class="extlink">PubMed: 27738138</a> </td>
    <td> </td>
  </tr>
    <tr>
        <td>
            <p><i>C. auris</i></p>
        </td>
        <td>Genomic sequences and  annotations</td>
        <td><a href="https://www.ncbi.nlm.nih.gov/assembly/GCA_002759435.2/" target="_new" class="extlink">GenBank database.  (Accession : GCA_002759435.2)</a></td>
        <td><a href="https://pubmed.ncbi.nlm.nih.gov/27988485/" target="_new" class="extlink">PubMed: 27988485</a></td>
        <td> </td>
    </tr>

  <tr>
      <td rowspan="1"  class="leftcol"> <h2><p>Homology</p></h2></td>
      <td>
          <p><i>C. albicans</i></p>
          <p><i>C. glabrata</i></p>
          <p><i>C. parapsilosis</i></p>
          <p><i>C. tropicalis</i></p>
          <p><i>C. auris</i></p>
          <p><i>C. dubliniensis</i></p>
          <p><i>C. cerevisiae</i></p>
          <p><i>C. nidulans</i></p>
      </td>
      <td>Gene Orthologues</td>
      <td>
          <p><a href="http://www.candidagenome.org/" target="_new" class="extlink">Candida Genome Database (CGD)</a></p>
          <p><a href="http://cgob.ucd.ie/" target="_new" class="extlink">Candida Gene Order Browser (CGOB)</a></p>
      </td>
      <td>
          <p><a href="https://pubmed.ncbi.nlm.nih.gov/27988485/" target="_new" class="extlink">PubMed: 27988485</a></p>
          <p><a href="https://pubmed.ncbi.nlm.nih.gov/23486613/" target="_new" class="extlink">PubMed: 23486613</a></p>
      </td>
      <td></td>
  </tr>

    <tr>
        <td rowspan="3"  class="leftcol"> <h2><p>Proteins</p></h2></td>
        <td>
            <p><i>C. albicans</i></p>
            <p><i>C. glabrata</i></p>
            <p><i>C. parapsilosis</i></p>
            <p><i>C. tropicalis</i></p>
        </td>
        <td>Protein annotation</td>
        <td>
            <p><a href="https://www.uniprot.org/" target="_new" class="extlink">Uniprot</a></p>
        </td>
        <td>
            <p><a href="https://pubmed.ncbi.nlm.nih.gov/27899622/" target="_new" class="extlink">PubMed: 27899622</a></p>
        </td>
        <td></td>
    </tr>

    <tr>
        <td>
            <p><i>C. albicans</i></p>
            <p><i>C. glabrata</i></p>
            <p><i>C. parapsilosis</i></p>
            <p><i>C. tropicalis</i></p>
        </td>
        <td>Uniprot Proteins to Proteins Domains and Proteins Domains information</td>
        <td>
            <p><a href="https://www.ebi.ac.uk/interpro/" target="_new" class="extlink">Interpro</a></p>
        </td>
        <td>
            <p><a href="https://pubmed.ncbi.nlm.nih.gov/27899635/" target="_new" class="extlink">PubMed: 27899635</a></p>
        </td>
        <td></td>
    </tr>

    <tr>
        <td>
            <p><i>C. albicans</i></p>
            <p><i>C. glabrata</i></p>
            <p><i>C. parapsilosis</i></p>
            <p><i>C. tropicalis</i></p>
        </td>
        <td>Predicted Proteins Domains by IprScan </td>
        <td>
            <p><a href="http://www.candidagenome.org/" target="_new" class="extlink">Candida Genome Database (CGD)</a></p>
        </td>
        <td>
            <p><a href="https://pubmed.ncbi.nlm.nih.gov/27988485/" target="_new" class="extlink">PubMed: 27988485</a></p>
        </td>
        <td></td>
    </tr>


    <tr>
        <td rowspan="3"  class="leftcol"> <h2><p>Gene Annotation</p></h2></td>
        <td>
            <p><i>C. albicans</i></p>
            <p><i>C. glabrata</i></p>
            <p><i>C. parapsilosis</i></p>
            <p><i>C. tropicalis</i></p>
            <p><i>C. auris</i></p>
        </td>
        <td>Go Annotations</td>
        <td>
            <p><a href="http://www.candidagenome.org/" target="_new" class="extlink">CGD</a></p>
            <p><a href="https://www.ncbi.nlm.nih.gov/assembly/GCA_002759435.2/" target="_new" class="extlink">GenBank</a></p>
        </td>
        <td></td>
        <td></td>
    </tr>

    <tr>
        <td>
            <p><i>C. albicans</i></p>
            <p><i>C. glabrata</i></p>
            <p><i>C. parapsilosis</i></p>
        </td>
        <td>Phenotype Annotations</td>
        <td>
            <p><a href="http://www.candidagenome.org/" target="_new" class="extlink">CGD</a></p>
        </td>
        <td></td>
        <td></td>
    </tr>

    <tr>
        <td>
            <p><i>C. albicans</i></p>
            <p><i>C. glabrata</i></p>
            <p><i>C. tropicalis</i></p>
        </td>
        <td>Pathways Annotations</td>
        <td>
            <p><a href="https://www.genome.jp/kegg/" target="_new" class="extlink">Kyoto Encyclopedia of Genes and Genomes (KEGG)</a></p>
        </td>
        <td></td>
        <td></td>
    </tr>

    <tr>
        <td  class="leftcol"> <h2><p>Expressions</p></h2></td>
        <td>
            <p><i>C. albicans</i></p>
            <p><i>C. glabrata</i></p>
            <p><i>C. parapsilosis</i></p>
            <p><i>C. tropicalis</i></p>
            <p><i>C. auris</i></p>
        </td>
        <td>Gene Expressions profiles</td>
        <td>
            <p></p>
        </td>
        <td>
            <p></p>
        </td>
        <td></td>
    </tr>

    <tr>
        <td class="leftcol"> <h2><p>Variants</p></h2></td>
        <td>
            <p><i>C. albicans</i></p>
            <p><i>C. glabrata</i></p>
            <p><i>C. parapsilosis</i></p>
            <p><i>C. tropicalis</i></p>
            <p><i>C. auris</i></p>
        </td>
        <td>SNPs and Indels</td>
        <td>
            <p></p>
        </td>
        <td>
            <p></p>
        </td>
        <td></td>
    </tr>

    <tr>
        <td  class="leftcol"> <h2><p>Interaction</p></h2></td>
        <td>
            <p><i>C. albicans</i></p>
            <p><i>C. glabrata</i></p>
            <p><i>C. parapsilosis</i></p>
            <p><i>C. tropicalis</i></p>
        </td>
        <td>Protein to protein interactions</td>
        <td>
            <p><a href="http://www.candidagenome.org/" target="_new" class="extlink">STRING: functional protein association networks</a> STRING v11</p>
        </td>
        <td>
            <p><a href="https://pubmed.ncbi.nlm.nih.gov/30476243/" target="_new" class="extlink">PubMed: 30476243</a></p>
        </td>
        <td></td>
    </tr>

    <tr>
        <td  class="leftcol"> <h2><p>Metadata</p></h2></td>
        <td>
            <p><i>C. albicans</i></p>
            <p><i>C. glabrata</i></p>
            <p><i>C. parapsilosis</i></p>
            <p><i>C. tropicalis</i></p>
            <p><i>C. auris</i></p>
        </td>
        <td>Samples and Experiments Metadata</td>
        <td>
            <p><a href="https://www.ncbi.nlm.nih.gov/sra" target="_new" class="extlink">NCBI Sequence Read Archive (SRA)</a></p>
        </td>
        <td>
            <p><a href="https://pubmed.ncbi.nlm.nih.gov/21062823/" target="_new" class="extlink">PubMed: 21062823</a></p>

        </td>
        <td></td>
    </tr>

    <tr>
        <td  class="leftcol"> <h2><p>Publications</p></h2></td>
        <td></td>
        <td>Associated Publications from PubMed database</td>
        <td></td>
        <td></td>
        <td></td>
    </tr>

</table>

<div class="body">
<ol>
  <li><a name="note1">${note1}</a></li>
</ol>
</div>

</div>
<!-- /dataCategories -->
