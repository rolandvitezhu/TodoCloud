package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.appContext
import com.rolandvitezhu.todocloud.app.Constant
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.DatePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderDatePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderTimePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.*
import javax.inject.Inject

class CreateTodoFragment : Fragment() {

    @Inject
    lateinit var dbLoader: DbLoader

    @BindView(R.id.textinputlayout_createtodo_title)
    lateinit var tilTitle: TextInputLayout

    @BindView(R.id.textinputedittext_createtodo_title)
    lateinit var tietTitle: TextInputEditText

    @BindView(R.id.switch_createtodo_priority)
    lateinit var switchPriority: SwitchCompat

    @BindView(R.id.textview_createtodo_duedate)
    lateinit var tvDueDate: TextView

    @BindView(R.id.textview_createtodo_reminderdatetime)
    lateinit var tvReminderDateTime: TextView

    @BindView(R.id.button_createtodo_clearduedate)
    lateinit var btnClearDueDate: TextView

    @BindView(R.id.button_createtodo_clearreminder)
    lateinit var btnClearReminder: TextView

    @BindView(R.id.textinputedittext_createtodo_description)
    lateinit var tietDescription: TextInputEditText

    private var dueDate: LocalDate? = null
    private var zdtDueDate: ZonedDateTime? = null
    private var dueDateLong: Long = 0
    private var dueDateDisp: String? = null
    private var reminderDateTime: LocalDateTime? = null
    private var zdtReminderDateTime: ZonedDateTime? = null
    private var reminderDateTimeLong: Long = 0
    private var reminderDateTimeDisp: String? = null
    private lateinit var todosViewModel: TodosViewModel

    lateinit var unbinder: Unbinder

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (Objects.requireNonNull(activity)?.application as AppController).appComponent.
        fragmentComponent().create().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initDueDate()
        todosViewModel = ViewModelProviders.of(activity!!).get(TodosViewModel::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_createtodo, container, false)
        unbinder = ButterKnife.bind(this, view)

        tietTitle!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateTitle()
            }
        })
        setTvDueDateText(dueDate)
        setTvReminderDateTimeText(null)

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)!!.onSetActionBarTitle(getString(R.string.all_createtodo))
        setClearDueDateVisibility()
        setClearReminderDateTimeVisibility()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
    }

    private fun initDueDate() {
        if (dueDate == null) {
            dueDate = LocalDate.now()
            zdtDueDate = dueDate!!.atStartOfDay(ZoneId.systemDefault())
            dueDateLong = zdtDueDate!!.toInstant().toEpochMilli()
            dueDateDisp = DateUtils.formatDateTime(
                    appContext,
                    dueDateLong,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_YEAR
            )
        }
    }

    private fun initReminderDateTime() {
        if (reminderDateTime == null) {
            reminderDateTime = LocalDateTime.now()
            zdtReminderDateTime = reminderDateTime!!.atZone(ZoneId.systemDefault())
            reminderDateTimeLong = zdtReminderDateTime!!.toInstant().toEpochMilli()
            reminderDateTimeDisp = DateUtils.formatDateTime(
                    appContext,
                    reminderDateTimeLong,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME
            )
        }
    }

    private fun setClearDueDateVisibility() {
        if (dueDate != null) btnClearDueDate!!.visibility = View.VISIBLE else btnClearDueDate!!.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val givenTitle = tietTitle!!.text.toString().trim { it <= ' ' }
        if (!givenTitle.isEmpty()) {
            menu.clear()
            inflater.inflate(R.menu.fragment_createtodo, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val menuItemId = item.itemId
        if (menuItemId == R.id.menuitem_createtodo) {
            hideSoftInput()
            val todoToCreate = prepareTodoToCreate()
            todosViewModel!!.todo = todoToCreate
            (activity as MainActivity?)!!.CreateTodo()
            (activity as MainActivity?)!!.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun prepareTodoToCreate(): Todo {
        val todo = Todo()

        todo.title = tietTitle!!.text.toString().trim { it <= ' ' }
        todo.priority = switchPriority!!.isChecked
        todo.dueDate = dueDateLong
        todo.reminderDateTime = reminderDateTimeLong
        val givenDescription = tietDescription!!.text.toString().trim { it <= ' ' }
        if (givenDescription != "") {
            todo.description = tietDescription!!.text.toString()
        } else {
            todo.description = null
        }
        todo.completed = false
        todo.rowVersion = 0
        todo.deleted = false
        todo.dirty = true
        todo.position = dbLoader!!.nextFirstTodoPosition

        return todo
    }

    private fun hideSoftInput() {
        val activity = activity
        val inputMethodManager = activity!!.getSystemService(
                Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        val currentlyFocusedView = activity.currentFocus
        if (currentlyFocusedView != null) {
            val windowToken = currentlyFocusedView.windowToken
            inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    private fun validateTitle() {
        activity!!.invalidateOptionsMenu()
        val givenTitle = tietTitle!!.text.toString().trim { it <= ' ' }
        if (givenTitle.isEmpty()) tilTitle!!.error = getString(R.string.all_entertitle) else tilTitle!!.isErrorEnabled = false
    }

    private fun openDatePickerDialogFragment() {
        val arguments = Bundle()
        arguments.putSerializable(Constant.DUE_DATE, dueDate)
        val datePickerDialogFragment = DatePickerDialogFragment()
        datePickerDialogFragment.setTargetFragment(this, 0)
        datePickerDialogFragment.arguments = arguments
        datePickerDialogFragment.show(fragmentManager!!, "DatePickerDialogFragment")
    }

    private fun openReminderDatePickerDialogFragment() {
        val arguments = Bundle()
        arguments.putSerializable(Constant.REMINDER_DATE_TIME, reminderDateTime)
        val reminderDatePickerDialogFragment = ReminderDatePickerDialogFragment()
        reminderDatePickerDialogFragment.setTargetFragment(this@CreateTodoFragment, 0)
        reminderDatePickerDialogFragment.arguments = arguments
        reminderDatePickerDialogFragment.show(
                fragmentManager!!,
                "ReminderDatePickerDialogFragment"
        )
    }

    fun onSelectDate(date: LocalDate?) {
        setTvDueDateText(date)
        setClearDueDateVisibility()
    }

    private fun setTvDueDateText(date: LocalDate?) {
        if (date != null) {
            dueDate = date
            zdtDueDate = dueDate!!.atStartOfDay(ZoneId.systemDefault())
            dueDateLong = zdtDueDate!!.toInstant().toEpochMilli()
            dueDateDisp = DateUtils.formatDateTime(
                    appContext,
                    dueDateLong,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_YEAR
            )
            tvDueDate!!.text = dueDateDisp
        } else {
            tvDueDate!!.setText(R.string.all_noduedate)
        }
    }

    fun onSelectReminderDate(date: LocalDateTime) {
        openReminderTimePickerDialogFragment(date)
    }

    private fun openReminderTimePickerDialogFragment(date: LocalDateTime) {
        val arguments = Bundle()
        arguments.putSerializable(Constant.REMINDER_DATE_TIME, date)
        val reminderTimePickerDialogFragment = ReminderTimePickerDialogFragment()
        reminderTimePickerDialogFragment.setTargetFragment(this, 0)
        reminderTimePickerDialogFragment.arguments = arguments
        reminderTimePickerDialogFragment.show(
                fragmentManager!!,
                "ReminderTimePickerDialogFragment"
        )
    }

    fun onDeleteReminder() {
        tvReminderDateTime!!.text = getString(R.string.all_noreminder)
    }

    fun onSelectReminderDateTime(date: LocalDateTime?) {
        setTvReminderDateTimeText(date)
        setClearReminderDateTimeVisibility()
    }

    private fun setClearReminderDateTimeVisibility() {
        if (reminderDateTime != null) btnClearReminder!!.visibility = View.VISIBLE else btnClearReminder!!.visibility = View.GONE
    }

    private fun setTvReminderDateTimeText(date: LocalDateTime?) {
        if (date != null) {
            reminderDateTime = date
            zdtReminderDateTime = reminderDateTime!!.atZone(ZoneId.systemDefault())
            reminderDateTimeLong = zdtReminderDateTime!!.toInstant().toEpochMilli()
            reminderDateTimeDisp = DateUtils.formatDateTime(
                    appContext,
                    reminderDateTimeLong,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME
            )
            tvReminderDateTime!!.text = reminderDateTimeDisp
        } else {
            tvReminderDateTime!!.setText(R.string.all_noreminder)
        }
    }

    private fun clearDueDate() {
        tvDueDate!!.setText(R.string.all_noduedate)
        dueDate = null
        zdtDueDate = null
        dueDateLong = 0
        dueDateDisp = null
    }

    private fun clearReminder() {
        tvReminderDateTime!!.setText(R.string.all_noreminder)
        reminderDateTime = null
        zdtReminderDateTime = null
        reminderDateTimeLong = 0
        reminderDateTimeDisp = null
    }

    @OnClick(R.id.textview_createtodo_duedate)
    fun onTvDueDateClick(view: View?) {
        openDatePickerDialogFragment()
    }

    @OnClick(R.id.button_createtodo_clearduedate)
    fun onBtnClearDueDateClick(view: View?) {
        clearDueDate()
        setClearDueDateVisibility()
    }

    @OnClick(R.id.button_createtodo_clearreminder)
    fun onBtnClearReminderClick(view: View?) {
        clearReminder()
        setClearReminderDateTimeVisibility()
    }

    @OnClick(R.id.textview_createtodo_reminderdatetime)
    fun onTvReminderDateTimeClick(view: View?) {
        openReminderDatePickerDialogFragment()
    }
}