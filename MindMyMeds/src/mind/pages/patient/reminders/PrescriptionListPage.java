package mind.pages.patient.reminders;

import java.util.*;

import mind.database.*;
import mind.pages.DrugChooserTypeAhead;
import mind.pages.patient.PatientPage;
import mind.pages.patient.coaching.DrugInfoPage;
import mind.pages.patient.coaching.DrugInteractionPage;
import mind.tasks.GenerateNewDosesRecurringTask;
import samoyan.controls.ViewTableControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.RedirectException;
import samoyan.tasks.TaskManager;

public class PrescriptionListPage extends PatientPage
{
	public final static String COMMAND = RemindersPage.COMMAND + "/rx-list";

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		Patient patient = PatientStore.getInstance().loadByUserID(ctx.getUserID());
		
		boolean wake = false;
		
		for (String prmName : getContext().getParameterNamesThatStartWith("chk_"))
		{
			UUID rxID = UUID.fromString(prmName.substring(4));
			Prescription rx = PrescriptionStore.getInstance().open(rxID);
			if (rx.getPatientID().equals(patient.getID())==false)
			{
				continue;
			}
			
			if (isParameter("delete"))
			{
				PrescriptionStore.getInstance().remove(rxID);
			}
			else if (Setup.isDebug() && isParameter("trigger"))
			{
				rx.setNextDoseDate(new Date());
				PrescriptionStore.getInstance().save(rx);
				
				wake = true;
			}
		}
		
		if (wake)
		{
			TaskManager.runOnce(new GenerateNewDosesRecurringTask());
		}
		
		// Redirect to self in order to clear form submission
		throw new RedirectException(ctx.getCommand(), null);
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("mind:RxList.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		// Load prescriptions for this user
		RequestContext ctx = getContext();
		Patient patient = PatientStore.getInstance().loadByUserID(ctx.getUserID());
		List<UUID> rxIDs = PrescriptionStore.getInstance().getByPatientID(patient.getID());
		
		// Add
		writeFormOpen("GET", EditPrescriptionPage.COMMAND);
//		write("<table width=\"100%\"><tr valign=middle><td width=\"1%\">");
		writeTypeAheadInput("drug", null, null, 16, Drug.MAXSIZE_NAME, getPageURL(DrugChooserTypeAhead.COMMAND));
//		write("</td><td width=\"1%\">");
		write(" ");
		writeButton(getString("controls:Button.Add"));
//		write("</td>");
//		if (rxIDs.size()>0 && !ctx.getUserAgent().isSmartPhone())
//		{
//			write("<td align=right width=\"98%\">");
//			
//			// Excel icon (functionality not implemented !$!)
//			String excelUrl = getPageURL(COMMAND, new ParameterMap(ctx.getParameters()).plus("excel", "1"));
//			writeImage("excel-icon.png", getString("admin:OutgoingSMS.DownloadToExcel"), excelUrl);
//			write(" ");
//			writeLink(getString("admin:OutgoingSMS.DownloadToExcel"), excelUrl);
//
//			write("</td>");
//		}	
//		write("</tr></table>");
		writeFormClose();
		write("<br>");

		if (rxIDs.size()==0)
		{
			writeEncode(getString("mind:RxList.NoPrescriptions"));
			return;
		}
		
		// Load all Prescriptions and sort
		final List<Prescription> rxs = new ArrayList<Prescription>(rxIDs.size());
		for (int m=0; m<2; m++)
		{
			for (UUID rxID : rxIDs)
			{
				Prescription rx = PrescriptionStore.getInstance().load(rxID);
				
				// First, print drugs that have doses remaining
				// Second, list drugs with no doses remaining
				if (m==0 && rx.getDosesRemaining()<=0)
				{
					continue;
				}
				else if (m==1 && rx.getDosesRemaining()>0)
				{
					continue;
				}
				
				rxs.add(rx);
			}
		}
		
		writeFormOpen();
		
		new ViewTableControl<Prescription>(this, "rxs", rxs)
		{
			private boolean smartPhone = getContext().getUserAgent().isSmartPhone();
			
			@Override
			protected void renderHeaders() throws Exception
			{
				cell();
				cellWidth(50);
				writeEncode(getString("mind:RxList.Drug"));
				cell(getString("mind:RxList.NextDose"));
				cell(getString("mind:RxList.Doses"));
				if (!smartPhone)
				{
					cell(getString("mind:RxList.Doctor"));
				}
			}

			@Override
			protected void renderRow(Prescription rx) throws Exception
			{
				Drug drug = DrugStore.getInstance().load(rx.getDrugID());

				// Checkbox
				cell();
				writeCheckbox("chk_" + rx.getID().toString(), null, false);
				
				// Drug info
				cell();
				writeLink(drug.getDisplayName(), getPageURL(EditPrescriptionPage.COMMAND, new ParameterMap("id", rx.getID().toString())));
				if (!Util.isEmpty(rx.getDoseInfo()))
				{
					if (!smartPhone)
					{
						write(" (");
						writeEncode(rx.getDoseInfo());
						write(")");
					}
					else
					{
						write("<br>");
						writeEncode(rx.getDoseInfo());
					}
				}
				if (!smartPhone && !Util.isEmpty(drug.getDescription()))
				{
					write("<br><div class=HalfLineSpace></div><small>");
					writeEncode(drug.getDescription());
					write(" ");
					writeLink(getString("mind:RxList.DrugInfo"), getPageURL(DrugInfoPage.COMMAND, new ParameterMap("id", drug.getID().toString())));
					write("</small>");
				}
				
//				// !$! hack for demo
//				if (smartPhone && !Util.isEmpty(rx.getDoctorName()))
//				{
//					write("<br>");
//					writeEncode(rx.getDoctorName());
//					
//					write(" ");
//					writeImage("mind/globe.png", "", "http://maps.google.com/maps?q=7601+Stoneridge+Drive++Pleasanton,+CA+94588");
//					write(" ");
//					writeImage("mind/phone.png", "", "tel:+19258475100");
//				}
				
	//			if (!Util.isEmpty(rx.getPurpose()))
	//			{
	//				write("<br>");
	//				writeEncode(rx.getPurpose());
	//			}
	
				// Next dose
				cell();
				if (rx.getDosesRemaining()>0)
				{
					writeEncodeMiniDate(rx.getNextDoseDate());
					write(" ");
					writeEncodeMiniTime(rx.getNextDoseDate());
				}
				
				// Remaining
				cell();
				if (rx.getDosesRemaining()>0)
				{
					writeEncodeLong(rx.getDosesRemaining());
				}
				else
				{
					write("<span class=Faded>");
					writeEncode(getString("mind:RxList.NoDosesRemaining"));
					write("</span>");
					
//					// !$! hack for demo
//					write(" ");
//					writeLink(getString("mind:RxList.Refill"), getPageURL(DrugInfoPage.COMMAND, new ParameterMap("id", drug.getID().toString())));
				}
				
				// Doctor
				if (!smartPhone)
				{
					cell();
					if (!Util.isEmpty(rx.getDoctorName()))
					{
						writeEncode(rx.getDoctorName());
						
//						// !$! hack for demo
//						write(" ");
//						writeImage("mind/globe.png", "", "http://maps.google.com/maps?q=7601+Stoneridge+Drive++Pleasanton,+CA+94588");
//						write(" ");
//						writeImage("mind/phone.png", "", "tel:+19258475100");
					}
				}
								
				// Drug interactions
				List<Drug> interactions = new ArrayList<Drug>();
				for (Prescription rx2 : rxs)
				{
					if (DrugStore.getInstance().isInteraction(drug.getID(), rx2.getDrugID()))
					{
						interactions.add(DrugStore.getInstance().load(rx2.getDrugID()));
					}
				}
				
				if (interactions.size()>0)
				{
					ParameterMap params = new ParameterMap();
					params.plus("d0", drug.getID().toString());
	
					StringBuffer drugNames = new StringBuffer(128);
					for (int x=0; x<interactions.size(); x++)
					{
						if (x>0)
						{
							drugNames.append(", ");
						}
						drugNames.append(interactions.get(x).getName());
						
						params.plus("d"+(x+1), interactions.get(x).getID().toString());
					}
					
					row();
					cell();
					cellSpan(smartPhone?3:4);

					write("<table><tr valign=middle><td>");
					writeImage("mind/drug-interaction-alert.png", null);
					write("</td><td>");
					write("<a href=\"");
					write(getPageURL(DrugInteractionPage.COMMAND, params));
					write("\" class=Red>");
					writeEncode(getString("mind:RxList.DrugInteraction", drugNames));
					write("</a>");
					write("</td></tr></table>");
				}
				
				row();
				cellSpan(smartPhone?4:5);
				write("&nbsp;");
			}
		}.setWidth(100).render();
		
		write("<br>");
		writeRemoveButton("delete");
		if (Setup.isDebug())
		{
			write("&nbsp;");
			writeButton("trigger", getString("mind:RxList.TriggerNow"));
		}
		writeFormClose();
	}
}
