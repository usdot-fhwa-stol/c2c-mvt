let oLoadedVersions = {};
let oLoadedEncodings = {};
let oLoadedMessageTypes = {};
let sPrevText = '';
let oFileToUpload = null;
let nPollInterval = 1000;
let bValidating = false;
let sC2CMvtNull = 'c2cmvtnull';

function init()
{
	$.ajax(
	{
		'url': 'standards',
		'method': 'GET',
		'dataType': 'JSON'
	}).done(function (oData) // oData is a JSON Array of strings representing the available standards
	{
		let sOptions = `<option value="${sC2CMvtNull}"></option>`;
		for (let sStandard of oData.values())
		{
			sOptions += `<option value="${sStandard}">${sStandard}</option>`;
		}
		let oSelectStandard = $('#select_standard');
		oSelectStandard.append(sOptions);
		oSelectStandard.on('change', resetOptions);
		oSelectStandard.on('change', setVersions);
		$('#select_version').on('change', setEncodings);
		$('#select_version').on('change', setMessageTypes);
	});
	checkMessages();
	
	$('#btnChooseFile').on('click', function()
	{
		$('#file_to_upload').click();
	});
	$('#file_to_upload').on('change', function()
	{
		oFileToUpload = this.files[0];
		uploadFile();
	});
	
	$('.file_textarea,.cannotdrop_file,.drop_file,.cannoutdrop_file').on( 'drag dragstart dragend dragover dragenter dragleave drop', function(oEvt)
	{
		// preventing the unwanted behaviours
		oEvt.preventDefault();
		oEvt.stopPropagation();
	}).on('dragenter', function()
	{
		$('.file_textarea').hide();
		if (optionsNotSet())
			$('.cannotdrop_file').css('display', 'flex');
		else
			$('.drop_file').css('display', 'flex');
	}).on('dragover dragenter', function()
	{
		$('.file_textarea,.cannotdrop_file,.drop_file,.cannoutdrop_file').addClass('isDragging');
	}).on('dragleave dragend drop', function()
	{
		$('.file_textarea,.cannotdrop_file,.drop_file,.cannoutdrop_file').removeClass('isDragging');
	}).on('dragleave', function()
	{
		oFileToUpload = null;
		$('.file_textarea').show();
		$('.cannotdrop_file').css('display', 'none');
		$('.drop_file').css('display', 'none');
	}).on('drop', function (oEvt)
	{
		if (optionsNotSet())
		{
			setTimeout(() => 
			{
				$('.cannotdrop_file').css('display', 'none');
				$('.file_textarea').show();
			}, 1500);
			return;
		}
		oFileToUpload = oEvt.originalEvent.dataTransfer.files[0];
		uploadFile();
	});
	
	$('#btnValidate').on('click', uploadFile);
	$('#btnResetLog').on('click', resetLog);
	$('#btnDownloadLog').on('click', downloadLog);
	$('select').on('change', setButtonDisabled);
	$('.file_textarea').on('keyup', setButtonDisabled);
}


function uploadFile()
{
	let oFormData = new FormData();
	if (oFileToUpload === null)
	{
		let sInput = $('.file_textarea').val();
		let oBlob = new Blob([sInput], { type: 'text/plain' });
		let oFile = new File([oBlob], 'upload.txt', {type: "text/plain"});
		oFormData.append('uploaded_file', oFile);
	}
	else
	{
		let oReader = new FileReader();
		oReader.addEventListener('load', function()
		{
			$('.file_textarea').val(oReader.result);
		});
		oReader.readAsText(oFileToUpload);
		oFormData.append('uploaded_file', oFileToUpload);
	}
	oFormData.append('standard', $('#select_standard').find(':selected').val());
	oFormData.append('version', $('#select_version').find(':selected').val());
	oFormData.append('encoding', $('#select_encoding').find(':selected').val());
	oFormData.append('message_type', $('#select_messagetype').find(':selected').val());
	bValidating = true;
	setButtonDisabled();

	// Perform AJAX request with jQuery
	$.ajax(
	{
		url: 'upload', // Replace with your server endpoint
		type: 'POST',
		data: oFormData,
		processData: false, // Prevent jQuery from processing FormData
		contentType: false, // Let FormData set the correct multipart headers
		headers: {'Last-Modified': new Date().toUTCString()},
		xhr: function () 
		{
			// Customize the XMLHttpRequest to track progress
			let xhr = new XMLHttpRequest();
			xhr.upload.addEventListener('progress', function (event) {
			if (event.lengthComputable) 
			{
				let percentComplete = (event.loaded / event.total) * 100;
				$('#progressBar').val(percentComplete);
			}
			});
			return xhr;
		},
		success: function (response) 
		{
			$('#progressBar').val(100).parent().append('<div id="upload_complete">Upload complete</div>');

		},
		error: function (jqXHR, textStatus, errorThrown) 
		{
			$('#progressBar').val(0).parent().append('<div id="upload_complete">Upload failed</div>');
		},
		complete: function()
		{
			setTimeout(() => 
			{
				$('#upload_complete').remove();
				$('.file_uploading').hide();
				$('.validating').css('display', 'flex');
				checkValidating();
			}, 1500);
		}
	});
	oFileToUpload = null;
	$('.file_textarea').hide();
	$('.drop_file').css('display', 'none');
	$('.cannotdrop_file').css('display', 'none');
	$('#progressBar').val(0);
	$('.file_uploading').show();
}



function setVersions()
{
	let sStandard = $('#select_standard').find(':selected').val();
	let oSelectVersion = $('#select_version');
	if (sStandard === sC2CMvtNull)
	{
		oSelectVersion.find('option').remove();
		$('#select_encoding').find('option').remove();
		$('#select_messagetype').find('option').remove();
		return;
	}
	let oVersions = oLoadedVersions[sStandard];
	if (oVersions === undefined)
	{
		$.ajax(
		{
			'url': 'versions',
			'method': 'POST',
			'dataType': 'JSON',
			'data': {'standard': sStandard}
		}).done(function (oData)
		{
			oLoadedVersions[sStandard] = oData;
			setVersions();
		});
	}
	else
	{
		let sOptions = `<option value="${sC2CMvtNull}"></option>`;
		for (let sVersion of oVersions.values())
		{
			sOptions += `<option value="${sVersion}">${sVersion}</option>`;
		}
		oSelectVersion.find('option').remove();
		oSelectVersion.append(sOptions);
	}
}


function setMessageTypes()
{
	let sStandard = $('#select_standard').find(':selected').val();
	let sVersion = $('#select_version').find(':selected').val();
	let oSelectMessageType = $('#select_messagetype');
	if (sVersion === sC2CMvtNull)
	{
		oSelectMessageType.find('option').remove();
		return;
	}
	let oStandardMessageTypes = oLoadedMessageTypes[sStandard];
	let oVersionMessageTypes;
	if (oStandardMessageTypes !== undefined)
	{
		oVersionMessageTypes = oStandardMessageTypes[sVersion];
	}
	else
	{
		oLoadedMessageTypes[sStandard] = {};
	}
	
	if (oVersionMessageTypes === undefined)
	{
		$.ajax(
		{
			'url': 'messagetypes',
			'method': 'POST',
			'dataType': 'JSON',
			'data': {'standard': sStandard, 'version': sVersion}
		}).done(function (oData)
		{
			oData.splice(0, 0, "Auto Detect");
			oLoadedMessageTypes[sStandard][sVersion] = oData;
			setMessageTypes();
		});
	}
	else
	{
		let sOptions = `<option value="${sC2CMvtNull}"></option>`;
		for (let sMessageType of oVersionMessageTypes.values())
		{
			sOptions += `<option value="${sMessageType}">${sMessageType}</option>`;
		}
		
		oSelectMessageType.find('option').remove();
		oSelectMessageType.append(sOptions);
	}
}


function setEncodings()
{
	let sStandard = $('#select_standard').find(':selected').val();
	let sVersion = $('#select_version').find(':selected').val();
	let oSelectEncoding = $('#select_encoding');
	if (sVersion === sC2CMvtNull)
	{
		oSelectEncoding.find('option').remove();
		return;
	}
	let oStandardEncodings = oLoadedEncodings[sStandard];
	let oVersionEncodings;
	if (oStandardEncodings !== undefined)
	{
		oVersionEncodings = oStandardEncodings[sVersion];
	}
	else
	{
		oLoadedEncodings[sStandard] = {};
	}
	
	if (oVersionEncodings === undefined)
	{
		$.ajax(
		{
			'url': 'encodings',
			'method': 'POST',
			'dataType': 'JSON',
			'data': {'standard': sStandard, 'version': sVersion}
		}).done(function (oData)
		{
			oLoadedEncodings[sStandard][sVersion] = oData;
			setEncodings();
		});
	}
	else
	{
		let sOptions = `<option value="${sC2CMvtNull}"></option>`;
		for (let sEncoding of oVersionEncodings.values())
		{
			sOptions += `<option value="${sEncoding}">${sEncoding}</option>`;
		}
		
		oSelectEncoding.find('option').remove();
		oSelectEncoding.append(sOptions);
	}
}


function resetOptions()
{
	$('#select_version,#select_encoding,#select_messagetype').find('option').remove();
}

function checkValidating()
{
	$.ajax(
	{
		'url': 'status',
		'method': 'POST',
		'dataType': 'JSON',
		'data': {'include_validation_records': false}
	}).done(doneStatus);
}


function checkMessages()
{
	$.ajax(
	{
		'url': 'status',
		'method': 'POST',
		'dataType': 'JSON',
		'data': {'include_validation_records': true}
	}).done(doneStatus);
}


function doneStatus(oData)
{
	if (oData.messages)
	{
		let sVal = '';
		for (let sMsg of oData.messages.values())
		{
			sVal += sMsg + '\n';
		}
		$('.msgcontainer').val(sVal);
	}
	if (oData.validating)
	{
		bValidating = true;
		$('.validating').css('display', 'flex');
		$('.file_textarea').hide();
		setButtonDisabled();
		setTimeout(checkValidating, 1000);
	}
	else
	{
		if (oData.messages)
		{
			bValidating = false;
			setButtonDisabled();
			$('.validating').css('display', 'none');
			$('.file_textarea').show();
		}
		else
		{
			checkMessages();
		}
	}
}

function optionsNotSet()
{
	return isUndefinedNullOrEmpty($('#select_standard').find(':selected').val()) || 
			isUndefinedNullOrEmpty($('#select_version').find(':selected').val())|| 
			isUndefinedNullOrEmpty($('#select_encoding').find(':selected').val())|| 
			isUndefinedNullOrEmpty($('#select_messagetype').find(':selected').val());
}

function setButtonDisabled()
{
	if (bValidating)
	{
		$('button').prop('disabled', true);
	}
	else
	{
		let bOptionsNotSet = optionsNotSet();
		$('#btnChooseFile').prop('disabled', bOptionsNotSet);
		$('#btnValidate').prop('disabled', bOptionsNotSet || $('.file_textarea').val().length === 0);
		$('button.log').prop('disabled', $('.msgcontainer').val().length === 0);
	}
}

function isUndefinedNullOrEmpty(sStr)
{
	return sStr === undefined || sStr === null || sStr.length === 0 || sStr === sC2CMvtNull;
}
function resetLog()
{
	$.ajax(
	{
		'url': 'resetLog',
		'method': 'GET',
		'dataType': 'JSON'
	});
	$('.msgcontainer').val('');
	setButtonDisabled();
}


function downloadLog()
{
	$.ajax(
	{
		'url': 'downloadLog',
		'method': 'GET',
		'xhrFields': {'responseType': 'blob'},
		'success': function(oBlob, sStatus, oXhr)
		{
			let sDisposition = oXhr.getResponseHeader('Content-Disposition');
			if (!sDisposition.startsWith('attachment; filename="')) // how the server creates the header this should always be false
				return;
			
			let sFilename = sDisposition.substring('attachment; filename="'.length, sDisposition.length - 1);
			if (typeof window.navigator.msSaveBlob !== 'undefined') // IE workaround
				window.navigator.msSaveBlob(oBlob, sFilename);
			else
			{
				let oURL = window.URL || window.webkitURL;
				let oDownload = oURL.createObjectURL(oBlob);
				if (sFilename)
				{
					let oA = document.createElement('a');
					if (typeof oA.download === 'undefined') // safari doesn't support download
						window.location.href = oDownload;
					else
					{
						oA.href = oDownload;
						oA.download = sFilename;
						document.body.appendChild(oA);
						oA.click();
					}
				}
				else
					window.location.href = oDownload;
				
				setTimeout(() => oURL.revokeObjectURL(oDownload), 100);
			}
		}
	});
}


$(document).ready(init);
