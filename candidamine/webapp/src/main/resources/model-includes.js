
(jQuery(function() { // run when the page has loaded.
  if (intermine) {   // but only if there is something to do.
    intermine.setOptions({CellPreviewTrigger: 'hover'});
    intermine.setOptions({
      'Location.start': true,
      'Location.end': true
    }, 'intermine.results.formatsets.genomic');

    intermine.setOptions({icons: 'fontawesome'}, '.Style');

intermine.scope('intermine.results.formatsets.genomic', {
  'Organism.name': true,
  'Organism.shortName': true
});

  }
});

