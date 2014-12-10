
function gen(p,d,str) {

    var f = 'form[name='+str+']';
    var ctx = $("#chart-"+str).get(0).getContext("2d");
    var chart = new Chart(ctx).Line(d);
    
    $(f).submit(function (e) {
	e.preventDefault();
    })
    

    
    function showValues() {
	var ser = $(f).serialize();
	$.getJSON( "/" + p +"?"+ser, function( ndata ) {
	    d.datasets[0].data = ndata.a;
	    d.datasets[1].data = ndata.b;  
	    chart.destroy;
	    $('#chart-'+str).remove();
	    $('#graph-container-'+str).append('<canvas id="chart-'+str+'" width="600" height="600"><canvas>');
	    var ctd = $("#chart-"+str).get(0).getContext("2d");
	    chart = new Chart(ctd).Line(d);
	});  
    }
    
    $(f + " > input[type='text']" ).change( function() { showValues() });
    showValues();
}

var dataa = {
    labels: ["January", "February", "March", "April", "May", "June", "July"],
    datasets: [
	{
	    label: "My First dataset",
	    fillColor: "rgba(220,220,220,0.2)",
	    strokeColor: "rgba(220,220,220,1)",
	    pointColor: "rgba(220,220,220,1)",
	    pointStrokeColor: "#fff",
	    pointHighlightFill: "#fff",
	    pointHighlightStroke: "rgba(220,220,220,1)",
	    data: [65, 59, 80, 81, 56, 55, 40]
	},
	{
	    label: "My Second dataset",
	    fillColor: "rgba(151,187,205,0.2)",
	    strokeColor: "rgba(151,187,205,1)",
	    pointColor: "rgba(151,187,205,1)",
	    pointStrokeColor: "#fff",
	    pointHighlightFill: "#fff",
	    pointHighlightStroke: "rgba(151,187,205,1)",
	    data: [28, 48, 40, 19, 86, 27, 90]
	}
    ]
};


var datab = {
    labels: ["January", "February", "March", "April", "May", "June", "July"],
    datasets: [
	{
	    label: "My First dataset",
	    fillColor: "rgba(220,220,220,0.2)",
	    strokeColor: "rgba(220,220,220,1)",
	    pointColor: "rgba(220,220,220,1)",
	    pointStrokeColor: "#fff",
	    pointHighlightFill: "#fff",
	    pointHighlightStroke: "rgba(220,220,220,1)",
	    data: [65, 59, 80, 81, 56, 55, 40]
	},
	{
	    label: "My Second dataset",
	    fillColor: "rgba(151,187,205,0.2)",
	    strokeColor: "rgba(151,187,205,1)",
	    pointColor: "rgba(151,187,205,1)",
	    pointStrokeColor: "#fff",
	    pointHighlightFill: "#fff",
	    pointHighlightStroke: "rgba(151,187,205,1)",
	    data: [28, 48, 40, 19, 86, 27, 90]
	}
    ]
};

gen("json", dataa, "a");
gen("json", datab, "b");      
