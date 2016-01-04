/**
 * Created by sjm on 11/6/15.
 */

var Format = {
    zeroPadLeft: function pad(n, width, z){
        z = z || '0';
        n = n + '';
        return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
    }
};

var console = {
    log: function(stMessage)
    {
        out.println(moment().format("YYYYMMDD-hhmmss")+": "+stMessage);
    }
};

function sayHello()
{
    console.log("Howdy from sayHello!!");

    return "42";
}

function enumerateAndPrint(theList) {
	theList.forEach( function( entry ) {
		console.log( "Item: " + entry );
	});
}

function scriptMain()
{
    console.log(Format.zeroPadLeft(1,4));
    console.log(Format.zeroPadLeft(1000000000,4));
    console.log(Format.zeroPadLeft(25,4));

    v1 = [ "Keith", "Ronnie", "Mick", "Charlie"];

    enumerateAndPrint(v1);
}
