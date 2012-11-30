package mind.pages.omh;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import mind.database.Dose;
import mind.database.DoseStore;
import mind.database.Drug;
import mind.database.DrugStore;
import mind.database.Patient;
import mind.database.PatientStore;
import mind.database.Prescription;
import mind.database.PrescriptionStore;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.servlet.RequestContext;
import samoyan.servlet.UrlGenerator;

public class ReadPage extends OmhPage
{
	public static final String COMMAND = "omh/v1.0/read";

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = null;

		// Authenticate
		String token = ctx.getParameter("auth_token");
		String username = ctx.getParameter("user");
		String password = ctx.getParameter("password");
		if (Util.isEmpty(token) == false)
		{
			user = authByToken(token, ctx.getUserAgent().getString());
		}
		else if (Util.isEmpty(username) == false && Util.isEmpty(password) == false)
		{
			user = authByUsername(username, password);
		}
		else
		{
			// Error
			writeError("must supply username/password or authentication token");
			statusCode = HttpServletResponse.SC_BAD_REQUEST;
			return;
		}

		if (user == null)
		{
			// Error
			writeError("unauthorized");
			statusCode = HttpServletResponse.SC_UNAUTHORIZED;
			return;
		}

		// Date range
		Date dtStart = getDate(ctx.getParameter("t_start"));
		Date dtEnd = getDate(ctx.getParameter("t_end"));
		if (dtStart != null && dtEnd != null && dtStart.after(dtEnd))
		{
			writeError("t_start must be earlier than t_end");
			statusCode = HttpServletResponse.SC_BAD_REQUEST;
			return;
		}

		// Paging numbers
		Integer numToSkip = getParameterInteger("num_to_skip");
		if (numToSkip == null)
		{
			numToSkip = 0;
		}
		Integer numToReturn = getParameterInteger("num_to_return");
		if (numToReturn == null)
		{
			numToReturn = 1000;
		}
		if (numToSkip < 0 || numToReturn < 0)
		{
			writeError("num_to_skip or num_to_return cannot be negative");
			statusCode = HttpServletResponse.SC_BAD_REQUEST;
			return;
		}

		// Get doses by user within the date range and paging numbers
		Patient patient = PatientStore.getInstance().loadByUserID(user.getID());
		List<UUID> doseIds = DoseStore.getInstance().getByPatient(patient.getID(), dtStart, dtEnd, null);

		write("{ ");

		// [start] metadata
		write("\"metadata\": { ");
		int count = Math.min(numToReturn, doseIds.size() - numToSkip);
		if (count < 0)
		{
			count = 0;
		}
		writeJsonIntAttr("count", count);

		Map<String, String> params = new HashMap<String, String>(ctx.getParameters());

		int prevNumToSkip = numToSkip - numToReturn;
		if (prevNumToSkip >= 0)
		{
			write(", ");
			params.put("num_to_skip", String.valueOf(prevNumToSkip));
			writeJsonStrAttr("previous", UrlGenerator.getPageURL(false, ctx.getHost(), COMMAND, params), false);
		}

		int nextNumToSkip = numToSkip + numToReturn;
		if (nextNumToSkip < doseIds.size())
		{
			write(", ");
			params.put("num_to_skip", String.valueOf(nextNumToSkip));
			writeJsonStrAttr("next", UrlGenerator.getPageURL(false, ctx.getHost(), COMMAND, params), false);
		}

		write(" }");
		// [end]

		// [start] data
		write(", ");
		write("\"data\": [ ");

		for (int i = numToSkip; i < numToSkip + numToReturn && i < doseIds.size(); i++)
		{
			UUID doseId = doseIds.get(i);

			if (i > numToSkip)
			{
				write(", ");
			}
			write("{ ");

			Dose dose = DoseStore.getInstance().load(doseId);
			Prescription prescription = PrescriptionStore.getInstance().load(dose.getPrescriptionID());
			Drug drug = DrugStore.getInstance().load(prescription.getDrugID());

			// [start] metadata
			write("\"metadata\": { ");
			writeJsonStrAttr("id", dose.getID().toString(), false);
			write(", ");
			writeJsonStrAttr("timestamp", df.format(new Date()), false);
			write(" }");
			// [end]

			// [start] data
			write(", ");
			write("\"data\": { ");

			// Columns: medicine name, *reminder sent (TakeDate), response time, y/n, reason.

			// medicine name
			writeJsonStrAttr("medicine_name", drug.getName(), true);

			// reminder sent
			write(", ");
			writeJsonStrAttr("reminder_sent", df.format(dose.getTakeDate()), false);

			// response
			int responseCode = dose.getResolution();
			String response;
			switch (responseCode)
			{
				case Dose.TAKEN:
					response = "Taken";
					break;
				case Dose.SKIPPED:
					response = "Skipped";
					break;
				default:
					response = "NoReply";
			}
			write(", ");
			writeJsonStrAttr("response", response, false);

			// response date
			if (dose.getResolutionDate() != null)
			{
				write(", ");
				writeJsonStrAttr("response_date", df.format(dose.getResolutionDate()), false);
			}

			// skip reason
			if (dose.getSkipReason() != null)
			{
				write(", ");
				writeJsonStrAttr("skip_reason", dose.getSkipReason(), true);
			}

			// doctor, instruction, and dose info
			write(", ");
			writeJsonStrAttr("doctor", prescription.getDoctorName(), true);
			write(", ");
			writeJsonStrAttr("instruction", prescription.getInstructions(), true);
			write(", ");
			writeJsonStrAttr("dose_info", prescription.getDoseInfo(), true);

			write(" }");
			// [end] data

			write(" }");
		}
		write(" ]");
		// [end] data

		write(" }");
	}

	private Date getDate(String date)
	{
		Date dt = null;

		if (Util.isEmpty(date) == false)
		{
			try
			{
				dt = df.parse(date);
			}
			catch (Exception e)
			{
				dt = null;
			}
		}

		return dt;
	}
}
