// APPAPI general interface
//

function doJsonRequest(stAction,jsonRequest)
{
	jsonRequest.$action = stAction;

	console.log("JSON: Sending request: "+stAction);

	$sr = APPAPI.jsJsonRequest(JSON.stringify(jsonRequest));

	if($sr == null)
	{
		$e = "JSON: null reply during action: "+stAction;

		console.log($e);

		throw $e;
	}

	console.log("JSON: Reply received, during: "+stAction);
	console.log("JSON:           Reply string: "+$sr);

	$r = JSON.parse($sr);

	switch( $r.$rc )
	{
		case -1 :		// N_RC_ERROR
		{
			$e = "JSON: ERROR reply: "+$r.$error;

			console.log($e);

			throw $e;
		}
		break;

		case 0 :		// N_RC_OK
		{
			console.log("JSON: Request SUCCESS!");
		}
		break;

		case 1 :		// N_RC_WARNING
		{
			console.log("JSON: Request WARNING: "+$r.$warning);
		}
		break;

		case 2 :		// N_RC_WARNING_NOTFOUND
		{
			console.log("JSON: Request NOTFOUND: "+$r.$warning);
		}
		break;

		case 3 :		// N_RC_USER_CANCELLED
		{
			console.log("JSON: Request USER CANCELLED!");
		}
		break;

		default :
		break;
	}
	return $r;
}

function doPageLoadRequest(stPageSpec)
{
	try
	{
		var $q = {
			"$pagespec" : stPageSpec
		};

		doJsonRequest("load-page",$q);
	}
	catch($err)
	{
		var $e = "LOAD-PAGE: caught: "+$err;

		console.log($e);

		throw $e;
	}
}

function doQueryById(stTableName,stRowId)
{
	// Add some stuff
	//
	try
	{
		$q = {
			"$tableName" : stTableName,
			"$rowId" : stRowId
		};

		return doJsonRequest("querybyid",$q);
	}
	catch($err)
	{
		var $e = "QUERYBYID: caught: "+$err;

		throw $e;
	}
}

function doShowUserNotification(stMessage)
{
	console.log("Showing user notification: "+stMessage);

	doJsonRequest("show-toast",{"$message" : stMessage});
}

function doStashStateToken(stToken,stValue)
{
	try
	{
		$q = {
			"$token" : stToken,
			"$value" : stValue
		};

		doJsonRequest("stash-state-token",$q);
	}
	catch($err)
	{
		var $e = "Stash token: caught: "+$err;

		throw $e;
	}
}

function doInAppPurchase(stIabAction,stSku,stTagString)
{
	console.log("Launch In-App Purchase: "+stSku);
	console.log("                Action: "+stIabAction);

	$q = {
		"$sku" : stSku,
		"$tag-string" : stTagString
	};

	$r = doJsonRequest(stIabAction,$q);

	console.log("IAB response: "+$r.$iabrc);

	return $r;
}

function doRestartActivity()
{
	doJsonRequest("restart-activity",{});
}

