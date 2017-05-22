package com.example.todocloud.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.app.AppController;
import com.example.todocloud.data.Todo;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.service.AlarmService;

import java.util.ArrayList;
import java.util.List;

public class TodoAdapter extends BaseAdapter {

	private List<Todo> todos;
  private DbLoader dbLoader;
  private Context context;

  public TodoAdapter(DbLoader dbLoader, Context context) {
    this.todos = new ArrayList<>();
    this.dbLoader = dbLoader;
    this.context = context;
  }

	@Override
  public int getCount() {
	  return todos.size();
  }

	@Override
  public Todo getItem(int position) {
	  return todos.get(position);
  }

  /**
   * Az adapterben kicseréli a megadott pozícióban lévő Todo-t a megadott Todo-ra.
   * @param position A pozíció, ahol a csere történik.
   * @param todo A Todo, amire a jelenlegit cseréljük.
   */
	public void setItem(int position, Todo todo) {
		todos.set(position, todo);
	}

  @Override
  public long getItemId(int position) {
	  return todos.get(position).get_id();
  }

	@Override
  public View getView(int position, View convertView, ViewGroup parent) {
	  
		final Todo todo = todos.get(position);
		
		LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
		View itemView = inflater.inflate(R.layout.todo_item, null);

    final AppCompatCheckBox cbCompleted = (AppCompatCheckBox) itemView.findViewById(
        R.id.cbCompleted);
    TextView tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
    TextView tvDueDate = (TextView) itemView.findViewById(R.id.tvDueDate);
    ImageView ivPriority = (ImageView) itemView.findViewById(R.id.ivPriority);

    cbCompleted.setChecked(todo.getCompleted());
    tvTitle.setText(todo.getTitle());
		tvDueDate.setText(todo.getDueDate());
    ivPriority.setVisibility(todo.isPriority() ? View.VISIBLE : View.INVISIBLE);

    // Az OnTouchListener miatt a setClickable(false) fölösleges.
    /*cbCompleted.setClickable(false);*/
    cbCompleted.setOnTouchListener(new View.OnTouchListener() {

      @Override
      public boolean onTouch(View v, MotionEvent event) {

        // Csak akkor legyen aktív a CheckBox, ha nem aktív az ActionMode.
        if (!AppController.isActionModeEnabled() && event.getAction() == MotionEvent.ACTION_UP)
          cbCompleted.setChecked(!cbCompleted.isChecked());

        return true;
      }

    });
    cbCompleted.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        todo.setCompleted(!todo.getCompleted());
        todo.setDirty(true);
        dbLoader.updateTodo(todo);
        deleteItem(todo);
        notifyDataSetChanged();

        if (todo.getCompleted()) {
          // A Todo elvégzett státuszú.
          // Emlékeztető törlése.
          Intent service = new Intent(context, AlarmService.class);
          service.putExtra("todo", todo);
          service.setAction(AlarmService.CANCEL);
          context.startService(service);
        } else if (!todo.getReminderDateTime().equals("-1")){
          // A Todo el nem végzett státuszú és rendelkezik beállított emlékeztetővel.
          // Emlékeztető élesítése.
          Intent service = new Intent(context, AlarmService.class);
          service.putExtra("todo", todo);
          service.setAction(AlarmService.CREATE);
          context.startService(service);
        }

      }

    });

	  return itemView;
  }

  /**
   * Frissíti az adapter tartalmát a megadott Todo-kkal.
   * @param todos A megadott Todo-k.
   */
  public void update(final ArrayList<Todo> todos) {
    this.todos.clear();
    this.todos.addAll(todos);
  }

  /**
   * Törli az adapterből a megadott Todo-t.
   * @param todo A törlendő Todo.
   */
	public void deleteItem(Todo todo){
		if (todos.contains(todo)) {
			todos.remove(todo);
		}
	}
	
}
