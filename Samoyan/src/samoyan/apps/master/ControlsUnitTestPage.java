package samoyan.apps.master;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import samoyan.controls.ControlArray;
import samoyan.controls.DataTableControl;
import samoyan.controls.DaysOfMonthChooserControl;
import samoyan.controls.GoogleGraph;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TabControl;
import samoyan.controls.TwoColFormControl;
import samoyan.servlet.WebPage;

public final class ControlsUnitTestPage extends WebPage
{
	public final static String COMMAND = "controls-unit-test";
	
	@Override
	public void renderHTML() throws Exception
	{
		List<Integer> nums = new ArrayList<Integer>();
		for (int i=0; i<10; i++)
		{
			nums.add(i);
		}

		write("\r\n<!--OpenForm-->");
		writeFormOpen();
		
		write("\r\n<!--LinkToolbar-->");
		new LinkToolbarControl(this)
			.addLink("Link 1", "http://www.example.com", "icons/standard/pencil-16.png")
			.addLink("Link 2", "http://www.example.com", "icons/standard/pencil-16.png")
			.render();
		
		write("\r\n<!--TabControl-->");
		new TabControl(this)
			.addTab("key1", "Tab 1", "http://www.example.com")
			.addTab("key2", "Tab 2", "http://www.example.com")
			.addTab("key3", "Tab 3", "http://www.example.com")
			.setCurrentTab("key1")
			.render();
		
		write("\r\n<!--TwoColFormControl-->Two column form");
		TwoColFormControl twoCol = new TwoColFormControl(this);

			twoCol.writeRow("Checkboxes");
			twoCol.write("\r\n<!--Checkboxes-->");
			twoCol.writeCheckbox("cb1", "Checkbox 1", false);
			twoCol.writeCheckbox("cb2", "Checkbox 2", true);

			twoCol.writeRow("Radios");
			twoCol.write("\r\n<!--Radio buttons-->");
			twoCol.writeRadioButton("radio", "Radio 1", "val1", "val1");
			twoCol.writeRadioButton("radio", "Radio 2", "val2", "val1");

			twoCol.writeRow("Date");
			twoCol.write("\r\n<!--DateInput-->");
			twoCol.writeDateInput("date", new Date());

			twoCol.writeRow("Date time");
			twoCol.write("\r\n<!--DateTimeInput-->");
			twoCol.writeDateTimeInput("datetime", new Date());

		twoCol.render();
			
		write("<br><br>");
		write("\r\n<!--DaysOfMonthChooser-->Days of month ");
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		int yyyy = cal.get(Calendar.YEAR);
		int mm = cal.get(Calendar.MONTH)+1;
		int dd = cal.get(Calendar.DAY_OF_MONTH);
		new DaysOfMonthChooserControl(this)
			.setMonth(yyyy, mm)
			.disableBefore(new Date())
			.select(dd)
			.setName("dayschooser")
			.render();
		
		write("<br><br>");
		write("\r\n<!--ImageInputControl-->Image upload ");
		writeImageInput("image", null);
		
		write("<br><br>");
		write("\r\n<!--NumberInputControl-->Number ");
		writeNumberInput("number", 0, 2, 0, 99);

		write("<br><br>");
		write("\r\n<!--PasswordInputControl-->Password ");
		writePasswordInput("pw", null, 8, 24);

		write("<br><br>");
		write("\r\n<!--PhoneInputControl-->Phone ");
		writePhoneInput("phone", "US/14085550000");

		write("<br><br>");
		write("\r\n<!--RichEditControl-->Rich edit ");
		writeRichEditField("rich", "<b>Rich edit</b> text with <i>formatting</i>", 80, 4);
		
		write("<br><br>");
		write("\r\n<!--TextAreaInput-->Text area ");
		writeTextAreaInput("textarea", "long text", 80, 4, 1024);
		
		write("<br><br>");
		write("\r\n<!--SelectInputControl-->Select ");
		new SelectInputControl(this, "select")
			.addOption("ff0000", "Red")
			.addOption("0000ff", "Blue")
			.addOption("00ff00", "Green")
			.setInitialValue("0000ff")
			.render();
		
		write("<br><br>");
		write("\r\n<!--ControlArray-->Control array");
		new ControlArray<Integer>(this, "ctrlarr", nums.subList(0, 3))
		{
			@Override
			public void renderRow(int rowNum, Integer rowElement) throws Exception
			{
				write("Text input ");
				writeTextInput("txt"+rowNum, rowNum>=0?rowNum:"", 8, 8);
			}
		}
		.render();
		
		write("<br><br>");
		write("\r\n<!--GoogleGraph-->");
		GoogleGraph gg = new GoogleGraph(this);
		gg.setHeight(200);
		gg.setWidth(400);
		gg.getChartArea().setRight(50);
		gg.addColumn(GoogleGraph.STRING, "Date");
		gg.addColumn(GoogleGraph.NUMBER, "Count");
		gg.addColumn(GoogleGraph.NUMBER, "Amount");
		gg.addRow("Jan", new Number[] {5, 4});
		gg.addRow("Feb", new Number[] {6, 4.5});
		gg.addRow("Mar", new Number[] {5.5, 5});
		gg.addRow("Apr", new Number[] {6.25, 5.25});
		gg.render();
		
		write("<br><br>");
		write("\r\n<!--DataTableControl-->");
		new DataTableControl<Integer>(this, "nums", nums)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column("Checkbox");
				column("Normal");
				column("Right aligned").align("right");
			}

			@Override
			protected void renderRow(Integer element) throws Exception
			{
				cell();
				writeCheckbox("numscb"+element, null, false);
				cell();
				writeEncodeLong(element);
				cell();
				writeEncodeLong(element*element);
			}
		}
		.setPageSize(5)
		.render();
		
		write("<br><br>");
		write("\r\n<!--SAVE BUTTON-->");
		writeSaveButton(null);
		write("\r\n<!--REMOVE BUTTON-->");
		writeRemoveButton();
		write("\r\n<!--BUTTON-->");
		writeButton("Button");
		
		write("\r\n<!--CloseForm-->");
		writeFormClose();
	}
}
