

// couchdb views, to be stored as _design/collect, viewname collect
//

// map
function(doc) { 
    emit(doc._id.substr(0,20), doc); 
    emit(doc._id.substr(0,25), doc); 
    emit(doc._id.substr(0,28), doc); 
    emit(doc._id.substr(0,31), doc); 

    emit('world-' + doc._id.substr(21,4), doc); 
    emit('world-' + doc._id.substr(21,7), doc); 
    emit('world-' + doc._id.substr(21,10), doc); 

    emit('world', doc);
} 

// reduce
function(keys, values, rereduce) { 
   var telapsed = 0;
   var total_count = 0;
   var total_lasociale = 0;
   for(var n in values) {
      total_lasociale += (values[n].lasociale || 0);

     
      telapsed += (values[n].elapsed || 0);
      total_count += (values[n].count || 1);
      
   }
   //telapsed = 9;
   return { lasociale: total_lasociale, 
        'elapsed': 4+telapsed,
            'count': total_count};
}
