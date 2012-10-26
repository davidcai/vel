$(document).ready(function()
{
	$(".RichEditCtrl IMG").click(richEditToolbarClick);
	$("INPUT[type=submit]").click(readAllRichEdits);
	$(".RichEditCtrl IFRAME").each(activateRichEdit);
});

function activateRichEdit(index)
{
	var doc = this.contentWindow.document;
	var content = this.previousSibling.value;

	doc.designMode = "On";

	doc.write(RICH_EDIT_DOC_START);
	doc.write(content);
	doc.write(RICH_EDIT_DOC_END);
	doc.close();

	if (doc.body!=null) // Firefox, Safari
	{
		try
		{
			doc.execCommand("useCSS", false, true);
			doc.execCommand("styleWithCSS", false, false);
		}
		catch (e)
		{
		}
	}
}

function readAllRichEdits(ev)
{
	$(".RichEditCtrl IFRAME").each(function(index) {
		this.previousSibling.value = this.contentWindow.document.body.innerHTML;
	});
}

function richEditToolbarClick(ev)
{
	var btn = ev.target;
	
	var applyTo = btn.getAttribute("applyto");
	var ctrl = document.getElementById("richedit_" + applyTo);
	var doc = ctrl.contentWindow.document;
	var cmd = btn.getAttribute("cmd");

	if (cmd=="createlink")
	{
		if (doc.selection) // IE
		{
			doc.execCommand("createlink", true, null);
		}
		else // Firefox, Safari
		{
			var anchor = getRichEditSelectedElement(applyTo, "A");
			var url = "http://";
			if (anchor!=null)
			{
				url = anchor.getAttribute("href");
			}

			url = prompt(RICH_EDIT_LINK_PROMPT, url);
			if (url!=null && url!="")
			{
				doc.execCommand("createlink", false, url);
			}
		}
	}
	else if (cmd=="expand")
	{
		var ht = Number(ctrl.style.height.substring(0, ctrl.style.height.length-2));
		ctrl.style.height = (ht+2) + "em";
		// ctrl.height = Number(ctrl.height)+54;
	}
	else
	{
		doc.execCommand(cmd, false, null);
	}
}

function getRichEditSelectedElement(applyTo, tagName)
{
	var win = document.getElementById("richedit_" + applyTo).contentWindow;
	var doc = win.document;

	if (doc.selection) // IE
	{
		var sel = doc.selection;
		var range = sel.createRange();
		try
		{
			var e = range.item(0);
			while (e!=null)
			{
				if (e.tagName==tagName) return e;
				e = e.parentNode;
			}
		}
		catch (e)
		{
		}
	}
	else // Firefox, Safari
	{
		var sel = win.getSelection();
		var range = sel.getRangeAt(0);
		
		var imgs = doc.getElementsByTagName(tagName);
		var i;
		for (i=0; i<imgs.length; i++)
		{
			if (sel.containsNode && sel.containsNode(imgs[i], false)) return imgs[i];
		}

		var e = range.startContainer;
		while (e!=null)
		{
			if (e.nodeType==1 && e.tagName==tagName) return e;
			e = e.parentNode;
		}
	}
	return null;
}
