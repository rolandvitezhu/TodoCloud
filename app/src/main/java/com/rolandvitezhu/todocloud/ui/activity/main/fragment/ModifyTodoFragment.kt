package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
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
import com.rolandvitezhu.todocloud.app.AppController.Companion.appContext
import com.rolandvitezhu.todocloud.app.AppController.Companion.setText
import com.rolandvitezhu.todocloud.app.Constant
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.DatePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderDatePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderTimePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import org.threeten.bp.*

class ModifyTodoFragment : Fragment() {

    @BindView(R.id.textinputlayout_modifytodo_title)
    lateinit var tilTitle: TextInputLayout

    @BindView(R.id.textinputedittext_modifytodo_title)
    lateinit var tietTitle: TextInputEditText

    @BindView(R.id.switch_modifytodo_priority)
    lateinit var switchPriority: SwitchCompat

    @BindView(R.id.textview_modifytodo_duedate)
    lateinit var tvDueDate: TextView

    @BindView(R.id.textview_modifytodo_reminderdatetime)
    lateinit var tvReminderDateTime: TextView

    @BindView(R.id.button_modifytodo_clearduedate)
    lateinit var btnClearDueDate: Button

    @BindView(R.id.button_modifytodo_clearreminder)
    lateinit var btnClearReminder: Button

    @BindView(R.id.textinputlayout_modifytodo_description)
    lateinit var tilDescription: TextInputLayout

    @BindView(R.id.textinputedittext_modifytodo_description)
    lateinit var tietDescription: TextInputEditText

    private lateinit var todo: Todo
    private var dueDate: LocalDate? = null
    private var zdtDueDate: ZonedDateTime? = null
    private var dueDateLong: Long = 0
    private var dueDateDisp: String? = null
    private var reminderDateTime: LocalDateTime? = null
    private var zdtReminderDateTime: ZonedDateTime? = null
    private var reminderDateTimeLong: Long = 0
    private var reminderDateTimeDisp: String? = null
    private var shouldNavigateBack: Boolean = false
    val isShouldNavigateBack: Boolean
        get() = shouldNavigateBack
    private lateinit var todosViewModel: TodosViewModel

    lateinit var unbinder: Unbinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        todosViewModel = ViewModelProviders.of(activity!!).get(TodosViewModel::class.java)
        todo = todosViewModel!!.todo
        initDueDate(todo)
        initReminderDateTime(todo)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_modifytodo, container, false)
        unbinder = ButterKnife.bind(this, view)

        setTvDueDateText(dueDate)
        setTvReminderDateTimeText(reminderDateTime)
        setText(todo!!.title, tietTitle!!, tilTitle!!)
        switchPriority!!.isChecked = todo!!.priority!!
        setText(todo!!.description, tietDescription!!, tilDescription!!)

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)!!.onSetActionBarTitle(getString(R.string.modifytodo_title))
        setClearDueDateVisibility()
        setClearReminderDateTimeVisibility()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
    }

    private fun setClearDueDateVisibility() {
        if (dueDate != null) btnClearDueDate!!.visibility = View.VISIBLE else btnClearDueDate!!.visibility = View.GONE
    }

    fun handleModifyTodo() {
        val rootView = view
        if (rootView != null) {
            hideSoftInput(rootView)
            var todo = todosViewModel!!.todo
            val titleOnUi = tietTitle!!.text.toString().trim { it <= ' ' }
            if (titleOnUi.isEmpty()) {
                val originalTitle = todo.title
                setText(originalTitle, tietTitle!!, tilTitle!!)
            } else {
                todo = prepareTodo(todo)
                todosViewModel!!.todo = todo
                (activity as MainActivity?)!!.ModifyTodo()
                shouldNavigateBack = true
                (activity as MainActivity?)!!.onBackPressed()
            }
        }
    }

    private fun prepareTodo(todo: Todo): Todo {
        val titleOnUi = tietTitle!!.text.toString().trim { it <= ' ' }
        val description = tietDescription!!.text.toString().trim { it <= ' ' }
        val priority = switchPriority!!.isChecked

        todo.dueDate = dueDateLong
        todo.title = titleOnUi
        todo.priority = priority
        todo.reminderDateTime = reminderDateTimeLong
        if (description != "") {
            todo.description = description
        }
        todo.dirty = true

        return todo
    }

    private fun hideSoftInput(rootView: View) {
        switchPriority!!.isFocusableInTouchMode = true
        switchPriority!!.requestFocus()
        val activity = activity
        val inputMethodManager = activity!!.getSystemService(
                Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        val windowToken = rootView.windowToken
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
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
        reminderDatePickerDialogFragment.setTargetFragment(this@ModifyTodoFragment, 0)
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

    private fun initDueDate(todo: Todo?) {
        if (todo!!.dueDate != null && todo.dueDate != 0L) {
            dueDate = Instant.ofEpochMilli(todo.dueDate!!).atZone(ZoneId.systemDefault()).toLocalDate()
            zdtDueDate = dueDate!!.atStartOfDay(ZoneId.systemDefault())
            dueDateLong = todo.dueDate!!
            dueDateDisp = DateUtils.formatDateTime(
                    appContext,
                    dueDateLong,
                    DateUtils.FORMAT_SHOW_DATE
                            or DateUtils.FORMAT_NUMERIC_DATE
                            or DateUtils.FORMAT_SHOW_YEAR
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
                    DateUtils.FORMAT_SHOW_DATE
                            or DateUtils.FORMAT_NUMERIC_DATE
                            or DateUtils.FORMAT_SHOW_YEAR
                            or DateUtils.FORMAT_SHOW_TIME
            )
        }
    }

    private fun initReminderDateTime(todo: Todo?) {
        if (todo!!.reminderDateTime != null && todo.reminderDateTime != 0L) {
            reminderDateTime = Instant.ofEpochMilli(todo.reminderDateTime!!)
                    .atZone(ZoneId.systemDefault()).toLocalDateTime()
            zdtReminderDateTime = reminderDateTime!!.atZone(ZoneId.systemDefault())
            reminderDateTimeLong = todo.reminderDateTime!!
            reminderDateTimeDisp = DateUtils.formatDateTime(
                    appContext,
                    reminderDateTimeLong,
                    DateUtils.FORMAT_SHOW_DATE
                            or DateUtils.FORMAT_NUMERIC_DATE
                            or DateUtils.FORMAT_SHOW_YEAR
                            or DateUtils.FORMAT_SHOW_TIME
            )
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

    @OnClick(R.id.textview_modifytodo_duedate)
    fun onDueDateClick(view: View?) {
        openDatePickerDialogFragment()
    }

    @OnClick(R.id.textview_modifytodo_reminderdatetime)
    fun onReminderDateTimeClick(view: View?) {
        openReminderDatePickerDialogFragment()
    }

    @OnClick(R.id.button_modifytodo_clearduedate)
    fun onBtnClearDueDateClick(view: View?) {
        clearDueDate()
        setClearDueDateVisibility()
    }

    @OnClick(R.id.button_modifytodo_clearreminder)
    fun onBtnClearReminderClick(view: View?) {
        clearReminder()
        setClearReminderDateTimeVisibility()
    }
}