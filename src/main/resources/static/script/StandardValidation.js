let oLoadedVersions = {};
let oLoadedEncodings = {};
let oLoadedMessageTypes = {};
let sPrevText = '';
let oFileToUpload = null;
let nPollInterval = 1000;


function init()
{
	$.ajax(
	{
		'url': 'standards',
		'method': 'GET',
		'dataType': 'JSON'
	}).done(function (oData) // oData is a JSON Array of strings representing the available standards
	{
		let sOptions = '<option value="0"></option>';
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
	
	$('.file_textarea').on( 'drag dragstart dragend dragover dragenter dragleave drop', function(oEvt)
	{
		// preventing the unwanted behaviours
		oEvt.preventDefault();
		oEvt.stopPropagation();
	}).on('dragenter', function()
	{
		sPrevText = $(this).val();
		$(this).val('');
	}).on('dragover dragenter', function()
	{
		$(this).addClass('isDragging');
	}).on('dragleave dragend drop', function()
	{
		$(this).removeClass('isDragging');
	}).on('dragleave', function()
	{
		$(this).val(sPrevText);
		sPrevText = '';
		oFileToUpload = null;
	}).on('drop', function (oEvt)
	{
		oFileToUpload = oEvt.originalEvent.dataTransfer.files[0];
		uploadFile();
	});
	
	$('#btnValidate').on('click', uploadFile);
	$('#btnResetLog').on('click', resetLog);
	$('#btnDownloadLog').on('click', downloadLog);
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
		oFormData.append('uploaded_file', oFileToUpload);
	}
	$('button').prop('disabled', true);

	// Perform AJAX request with jQuery
	$.ajax(
	{
		url: 'upload', // Replace with your server endpoint
		type: 'POST',
		data: oFormData,
		processData: false, // Prevent jQuery from processing FormData
		contentType: false, // Let FormData set the correct multipart headers
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
				$('.validating').show();
				checkMessages();
			}, 1500);
		}
	});
	oFileToUpload = null;
	sPrevText = '';
	$('.file_textarea').hide();
//	let oSuccess = $('.file_success');
//	oSuccess.html('').hide();
//	oSuccess[0].value = '';
	$('#progressBar').val(0);
	$('.file_uploading').show();
}

function displayFile()
{
	let sFilename = oFileToUpload.name;
	let oSuccess = $('.file_success');
	$('.file_textarea').hide();
	sPrevText = $('.file_textarea').val();
	oSuccess.show().html(`<strong>${sFilename}</strong> selected <i id="cancel_file" class="fa fa-times"></i>`);
	$('#cancel_file').on('click', function()
	{
		$('.file_textarea').show().val(sPrevText);
		oFileToUpload = null;
		sPrevText = '';
		oSuccess.html('').hide();
		oSuccess[0].value = '';
	});
}


function setVersions()
{
	let sStandard = $('#select_standard').val();
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
		let sOptions = '<option value="0"></option>';
		for (let sVersion of oVersions.values())
		{
			sOptions += `<option value="${sVersion}">${sVersion}</option>`;
		}
		let oSelectVersion = $('#select_version');
		oSelectVersion.find('option').remove();
		oSelectVersion.append(sOptions);
	}
}


function setMessageTypes()
{
	let sStandard = $('#select_standard').val();
	let sVersion = $('#select_version').val();
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
		let sOptions = '<option value="0"></option>';
		for (let sMessageType of oVersionMessageTypes.values())
		{
			sOptions += `<option value="${sMessageType}">${sMessageType}</option>`;
		}
		let oSelectMessageType = $('#select_messagetype');
		oSelectMessageType.find('option').remove();
		oSelectMessageType.append(sOptions);
	}
}


function setEncodings()
{
	let sStandard = $('#select_standard').val();
	let sVersion = $('#select_version').val();
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
		let sOptions = '<option value="0"></option>';
		for (let sEncoding of oVersionEncodings.values())
		{
			sOptions += `<option value="${sEncoding}">${sEncoding}</option>`;
		}
		let oSelectEncoding = $('#select_encoding');
		oSelectEncoding.find('option').remove();
		oSelectEncoding.append(sOptions);
	}
}


function resetOptions()
{
	$('#select_version,#select_encoding,#select_messagetype').find('option').remove();
}


function checkMessages()
{
	$.ajax(
	{
		'url': 'status',
		'method': 'GET',
		'dataType': 'JSON'
	}).done(function (oData)
	{
		let sVal = '';
		for (let sMsg of oData.messages.values())
		{
			sVal += sMsg + '\n';
		}
		$('.msgcontainer').val(sVal);
		if (oData.validating)
		{
			$('.validating').show();
			$('.file_textarea').hide();
			$('button').prop('disabled', true);
			setTimeout(checkMessages, 1000);
		}
		else
		{
			$('button').prop('disabled', false);
			$('.validating').hide();
			$('.file_textarea').show();
		}
	});
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
