package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import com.rolandvitezhu.todocloud.databinding.FragmentCreatetodoBinding
import com.rolandvitezhu.todocloud.helper.hideSoftInput
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.DatePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderDatePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderTimePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import kotlinx.android.synthetic.main.fragment_createtodo.*
import javax.inject.Inject

class CreateTodoFragment : Fragment() {

    @Inject
    lateinit var todoCloudDatabaseDao: TodoCloudDatabaseDao

    private val todosViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TodosViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as AppController).appComponent.
        fragmentComponent().create().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        todosViewModel.initializeTodo()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val fragmentCreatetodoBinding: FragmentCreatetodoBinding =
                FragmentCreatetodoBinding.inflate(inflater, container, false)
        val view: View = fragmentCreatetodoBinding.root

        fragmentCreatetodoBinding.lifecycleOwner = this
        fragmentCreatetodoBinding.createTodoFragment = this
        fragmentCreatetodoBinding.todosViewModel = todosViewModel
        fragmentCreatetodoBinding.executePendingBindings()

        return view
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).onSetActionBarTitle(getString(R.string.all_createtodo))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val title = this.textinputedittext_createtodo_title.text.toString().trim { it <= ' ' }
        if (title.isNotEmpty()) {
            menu.clear()
            inflater.inflate(R.menu.fragment_createtodo, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuitem_createtodo) {
            hideSoftInput()
            todosViewModel.onCreateTodo()
            (requireActivity() as MainActivity).onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun openDatePickerDialogFragment() {
        val datePickerDialogFragment = DatePickerDialogFragment()

        datePickerDialogFragment.setTargetFragment(this, 0)
        datePickerDialogFragment.show(parentFragmentManager, "DatePickerDialogFragment")
    }

    private fun openReminderDatePickerDialogFragment() {
        val reminderDatePickerDialogFragment = ReminderDatePickerDialogFragment()
        reminderDatePickerDialogFragment.
        setTargetFragment(this@CreateTodoFragment, 0)
        reminderDatePickerDialogFragment.show(
                parentFragmentManager,
                "ReminderDatePickerDialogFragment"
        )
    }

    fun onSelectReminderDate() {
        openReminderTimePickerDialogFragment()
    }

    private fun openReminderTimePickerDialogFragment() {
        val reminderTimePickerDialogFragment = ReminderTimePickerDialogFragment()

        reminderTimePickerDialogFragment.setTargetFragment(this, 0)
        reminderTimePickerDialogFragment.show(
                parentFragmentManager,
                "ReminderTimePickerDialogFragment"
        )
    }

    fun onTvDueDateClick() {
        openDatePickerDialogFragment()
    }

    fun onTvReminderDateTimeClick() {
        openReminderDatePickerDialogFragment()
    }
}