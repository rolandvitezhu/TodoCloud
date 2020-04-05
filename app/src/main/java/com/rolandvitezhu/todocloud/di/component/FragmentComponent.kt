package com.rolandvitezhu.todocloud.di.component

import com.rolandvitezhu.todocloud.di.FragmentScope
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.TodoAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.MoveListDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.*
import dagger.Subcomponent

@FragmentScope
@Subcomponent
interface FragmentComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): FragmentComponent
    }

    fun inject(mainListFragment: MainListFragment?)
    fun inject(loginUserFragment: LoginUserFragment?)
    fun inject(registerUserFragment: RegisterUserFragment?)
    fun inject(modifyPasswordFragment: ModifyPasswordFragment?)
    fun inject(resetPasswordFragment: ResetPasswordFragment?)
    fun inject(todoListFragment: TodoListFragment?)
    fun inject(searchFragment: SearchFragment?)
    fun inject(moveListDialogFragment: MoveListDialogFragment?)
    fun inject(todoAdapter: TodoAdapter?)
}