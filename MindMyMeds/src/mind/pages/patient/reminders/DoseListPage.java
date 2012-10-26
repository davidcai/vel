package mind.pages.patient.reminders;

import java.text.DateFormat;
import java.util.*;

import mind.database.*;
import mind.pages.patient.PatientPage;
import samoyan.controls.GoogleGraph;
import samoyan.controls.ViewTableControl;
import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
import samoyan.core.ReverseIterator;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;

public class DoseListPage extends PatientPage
{
	public final static String COMMAND = RemindersPage.COMMAND + "/dose-list";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("mind:DoseList.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		Patient patient = PatientStore.getInstance().loadByUserID(ctx.getUserID());
		final boolean smartPhone = ctx.getUserAgent().isSmartPhone();
		
		List<UUID> doseIDs = DoseStore.getInstance().getByPatient(patient.getID(), null, null, null);
		if (doseIDs.size()==0)
		{
			writeEncode(getString("mind:DoseList.NoDoses"));
			return;
		}
		
		// Graph
		if (!ctx.getUserAgent().isBlackBerry())
		{
			int countTaken = 0;
			int countSkipped = 0;
			int countUnresolved = 0;
			for (UUID doseID : doseIDs)
			{
				Dose dose = DoseStore.getInstance().load(doseID);
				if (dose.getResolution()==Dose.SKIPPED)
				{
					countSkipped++;
				}
				else if (dose.getResolution()==Dose.TAKEN)
				{
					countTaken++;
				}
				else
				{
					countUnresolved++;
				}
			}
			GoogleGraph graph = new GoogleGraph(this);
			graph.setChartType(GoogleGraph.PIE_CHART);
			graph.setLegend(GoogleGraph.NONE);
			graph.setHeight(150);
			graph.setWidth(150);
			graph.getChartArea().set(10,10,10,10);
			graph.addColumn(GoogleGraph.STRING, "");
			graph.addColumn(GoogleGraph.NUMBER, "");
			
			graph.addSlice().setColor("82C936");
			graph.addSlice().setColor("CC0909");
			graph.addSlice().setColor("3636C9");
	
			Number[] numberData = new Number[1];
			numberData[0] = countTaken;
			graph.addRow("Taken", numberData);
			numberData[0] = countSkipped;
			graph.addRow("Skipped", numberData);
			numberData[0] = countUnresolved;
			graph.addRow("Unresolved", numberData);
			graph.render();
			write("<br>");
		}
		
//		if (!ctx.getUserAgent().isSmartPhone())
//		{
//			// Excel icon (functionality not implemented !$!)
//			String excelUrl = getPageURL(COMMAND, new ParameterMap(ctx.getParameters()).plus("excel", "1"));
//			writeImage("excel-icon.png", getString("admin:OutgoingSMS.DownloadToExcel"), excelUrl);
//			write(" ");
//			writeLink(getString("admin:OutgoingSMS.DownloadToExcel"), excelUrl);
//			write("<br><br>");
//		}
		
		// Table
		new ViewTableControl<UUID>(this, "doses", new ReverseIterator<UUID>(doseIDs))
		{
			private Dose prevDose = null;
			private DateFormat dateFormat = smartPhone?
					DateFormatEx.getMiniDateInstance(getLocale(), getTimeZone()) :
					DateFormatEx.getDateInstance(getLocale(), getTimeZone());
			
			@Override
			protected void renderHeaders() throws Exception
			{
				cellWidth(1);
				writeEncode(getString("mind:DoseList.Date"));
				
				cellWidth(1);
				writeEncode(getString("mind:DoseList.Time"));
				
				cell();
				writeEncode(getString("mind:DoseList.Prescription"));
				
				if (!smartPhone)
				{
					cellSpan(2);
				}
				else
				{
					cell();
				}
				writeEncode(getString("mind:DoseList.Status"));
			}

			@Override
			protected void renderRow(UUID doseID) throws Exception
			{
				Dose dose = DoseStore.getInstance().load(doseID);
				Prescription rx = PrescriptionStore.getInstance().load(dose.getPrescriptionID());
				Drug drug = DrugStore.getInstance().load(rx.getDrugID());
								
				// Day
				cell();
				if (prevDose==null || sameDay(dose.getTakeDate(), prevDose.getTakeDate())==false)
				{
					writeEncode(dateFormat.format(dose.getTakeDate()));
				}
				
				// Time
				cell();
				if (prevDose==null || dose.getTakeDate().equals(prevDose.getTakeDate())==false)
				{
					writeEncodeMiniTime(dose.getTakeDate());
				}
				
				// Prescription
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
				
				// Status icon
				String url = getPageURL(DoseReminderNotif.COMMAND, new ParameterMap(DoseReminderNotif.PARAM_DOSE_ID, doseID.toString()));
				if (!smartPhone)
				{
					cell();
					if (dose.getResolution()==Dose.UNRESOLVED)
					{
						writeImage("mind/circle-qm.png", getString("mind:DoseList.Unresolved"), url);
					}
					else if (dose.getResolution()==Dose.TAKEN)
					{
						writeImage("mind/circle-v.png", getString("mind:DoseList.Taken"));
					}
					else if (dose.getResolution()==Dose.SKIPPED)
					{
						writeImage("mind/circle-x.png", getString("mind:DoseList.Skipped"));
					}
				}
				
				cell();
				if (dose.getResolution()==Dose.UNRESOLVED)
				{
					writeLink(getString("mind:DoseList.Unresolved"), url);
				}
				else if (dose.getResolution()==Dose.TAKEN)
				{
					writeEncode(getString("mind:DoseList.Taken"));
				}
				else if (dose.getResolution()==Dose.SKIPPED)
				{
					if (!Util.isEmpty(dose.getSkipReason()))
					{
						writeEncode(getString("mind:DoseList.Skipped"));
						if (!smartPhone)
						{
							write(", ");
							writeEncode(dose.getSkipReason());
						}
					}
					else
					{
						writeLink(getString("mind:DoseList.Skipped"), url);
					}
				}
				
				prevDose = dose;
			}
		}.render();		
	}
	
	private boolean sameDay(Date d1, Date d2)
	{
		Calendar c1 = Calendar.getInstance(getTimeZone());
		c1.setTime(d1);
		Calendar c2 = Calendar.getInstance(getTimeZone());
		c2.setTime(d2);

		return (c1.get(Calendar.DATE)==c2.get(Calendar.DATE) &&
				c1.get(Calendar.MONTH)==c2.get(Calendar.MONTH) &&
				c1.get(Calendar.YEAR)==c2.get(Calendar.YEAR));
	}
}
