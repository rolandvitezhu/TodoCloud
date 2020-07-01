package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.appContext
import com.rolandvitezhu.todocloud.app.Constant
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.databinding.FragmentCreatetodoBinding
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.DatePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderDatePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderTimePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import kotlinx.android.synthetic.main.fragment_createtodo.*
import kotlinx.android.synthetic.main.fragment_createtodo.view.*
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.*
import javax.inject.Inject

class CreateTodoFragment : Fragment() {

    @Inject
    lateinit var dbLoader: DbLoader

    private var dueDate: LocalDate? = null
    private var zdtDueDate: ZonedDateTime? = null
    private var dueDateLong: Long = 0
    private var dueDateDisp: String? = null
    private var reminderDateTime: LocalDateTime? = null
    private var zdtReminderDateTime: ZonedDateTime? = null
    private var reminderDateTimeLong: Long = 0
    private var reminderDateTimeDisp: String? = null
    private lateinit var todosViewModel: TodosViewModel

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
        val fragmentCreatetodoBinding: FragmentCreatetodoBinding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_createtodo, container, false)
        val view: View = fragmentCreatetodoBinding.root
        fragmentCreatetodoBinding.createTodoFragment = this

        view.textinputedittext_createtodo_title!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateTitle()
            }
        })
        setTvDueDateText(dueDate, view)
        setTvReminderDateTimeText(null, view)

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)!!.onSetActionBarTitle(getString(R.string.all_createtodo))
        setClearDueDateVisibility()
        setClearReminderDateTimeVisibility()
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
        if (dueDate != null)
            this.button_createtodo_clearduedate!!.visibility = View.VISIBLE
        else
            this.button_createtodo_clearduedate!!.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val givenTitle = this.textinputedittext_createtodo_title!!.text.toString().trim { it <= ' ' }
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

        todo.title = this.textinputedittext_createtodo_title!!.text.toString().trim { it <= ' ' }
        todo.priority = this.switch_createtodo_priority!!.isChecked
        todo.dueDate = dueDateLong
        todo.reminderDateTime = reminderDateTimeLong
        val givenDescription = this.textinputedittext_createtodo_description!!.text.toString().trim { it <= ' ' }
        if (givenDescription != "") {
            todo.description = this.textinputedittext_createtodo_description!!.text.toString()
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
        val givenTitle = this.textinputedittext_createtodo_title!!.text.toString().trim { it <= ' ' }
        if (givenTitle.isEmpty())
            this.textinputlayout_createtodo_title!!.error = getString(R.string.all_entertitle)
        else
            this.textinputlayout_createtodo_title!!.isErrorEnabled = false
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
        setTvDueDateText(date, null)
        setClearDueDateVisibility()
    }

    private fun setTvDueDateText(date: LocalDate?, view: View?) {
        if (date != null) {
            dueDate = date
            zdtDueDate = dueDate!!.atStartOfDay(ZoneId.systemDefault())
            dueDateLong = zdtDueDate!!.toInstant().toEpochMilli()
            dueDateDisp = DateUtils.formatDateTime(
                    appContext,
                    dueDateLong,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_YEAR
            )
            if (view != null)
                view.textview_createtodo_duedate!!.text = dueDateDisp
            else
                this.textview_createtodo_duedate!!.text = dueDateDisp
        } else {
            if (view != null)
                view.textview_createtodo_duedate!!.setText(R.string.all_noduedate)
            else
                this.textview_createtodo_duedate!!.setText(R.string.all_noduedate)
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
        this.textview_createtodo_reminderdatetime!!.text = getString(R.string.all_noreminder)
    }

    fun onSelectReminderDateTime(date: LocalDateTime?) {
        setTvReminderDateTimeText(date, null)
        setClearReminderDateTimeVisibility()
    }

    private fun setClearReminderDateTimeVisibility() {
        if (reminderDateTime != null)
            this.button_createtodo_clearreminder!!.visibility = View.VISIBLE
        else
            this.button_createtodo_clearreminder!!.visibility = View.GONE
    }

    private fun setTvReminderDateTimeText(date: LocalDateTime?, view: View?) {
        if (date != null) {
            reminderDateTime = date
            zdtReminderDateTime = reminderDateTime!!.atZone(ZoneId.systemDefault())
            reminderDateTimeLong = zdtReminderDateTime!!.toInstant().toEpochMilli()
            reminderDateTimeDisp = DateUtils.formatDateTime(
                    appContext,
                    reminderDateTimeLong,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME
            )
            if (view != null)
                view.textview_createtodo_reminderdatetime!!.text = reminderDateTimeDisp
            else
                this.textview_createtodo_reminderdatetime!!.text = reminderDateTimeDisp
        } else {
            if (view != null)
                view.textview_createtodo_reminderdatetime!!.setText(R.string.all_noreminder)
            else
                this.textview_createtodo_reminderdatetime!!.setText(R.string.all_noreminder)
        }
    }

    private fun clearDueDate() {
        this.textview_createtodo_duedate!!.setText(R.string.all_noduedate)
        dueDate = null
        zdtDueDate = null
        dueDateLong = 0
        dueDateDisp = null
    }

    private fun clearReminder() {
        this.textview_createtodo_reminderdatetime!!.setText(R.string.all_noreminder)
        reminderDateTime = null
        zdtReminderDateTime = null
        reminderDateTimeLong = 0
        reminderDateTimeDisp = null
    }

    fun onTvDueDateClick(view: View) {
        openDatePickerDialogFragment()
    }

    fun onBtnClearDueDateClick(view: View) {
        clearDueDate()
        setClearDueDateVisibility()
    }

    fun onBtnClearReminderClick(view: View) {
        clearReminder()
        setClearReminderDateTimeVisibility()
    }

    fun onTvReminderDateTimeClick(view: View) {
        openReminderDatePickerDialogFragment()
    }
}