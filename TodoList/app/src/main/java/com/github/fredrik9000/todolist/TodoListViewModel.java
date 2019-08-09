package com.github.fredrik9000.todolist;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.github.fredrik9000.todolist.model.Todo;
import com.github.fredrik9000.todolist.model.TodoRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

public class TodoListViewModel extends AndroidViewModel {
    private LiveData<List<Todo>> todoList;
    private Application application;
    private TodoRepository repository;

    public TodoListViewModel(@NonNull Application application) {
        super(application);
        repository = new TodoRepository(application);
        todoList = repository.getAllTodos();
        this.application = application;
    }

    public void insert(Todo todo) {
        repository.insert(todo);
    }

    public void delete(Todo todo) {
        repository.delete(todo);
    }

    public void update(Todo todo) {
        repository.update(todo);
    }

    public LiveData<List<Todo>> getTodoList() {
        return todoList;
    }

    public List<Todo> deleteTodosWithPriorities(ArrayList<Integer> priorities, AlarmManager alarmManager) {
        final List<Todo> removedTodoItems = new ArrayList<>();
        for (Iterator<Todo> iterator = todoList.getValue().listIterator(); iterator.hasNext(); ) {
            Todo todo = iterator.next();
            if (priorities.contains(todo.getPriority())) {
                removedTodoItems.add(todo);
                if (todo.isNotificationEnabled()) {
                    removeNotification(alarmManager, todo);
                }

                repository.delete(todo);
            }
        }
        return removedTodoItems;
    }

    public void insertTodoItems(List<Todo> todoListItems, AlarmManager alarmManager) {
        for (Iterator<Todo> iterator = todoListItems.listIterator(); iterator.hasNext(); ) {
            Todo todo = iterator.next();
            if (todo.isNotificationEnabled()) {
                addNotification(alarmManager, todo);
            }
        }
        repository.insertTodoItems(todoListItems);
    }

    public void removeNotification(AlarmManager alarmManager, Todo todo) {
        Intent notificationIntent = new Intent(application.getApplicationContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(application.getApplicationContext(), todo.getNotificationId(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    public void addNotification(AlarmManager alarmManager, Todo todo) {
        Intent notificationIntent = new Intent(application.getApplicationContext(), AlarmReceiver.class);
        notificationIntent.putExtra(AlarmReceiver.TODO_DESCRIPTION, todo.getDescription());
        PendingIntent broadcast = PendingIntent.getBroadcast(application.getApplicationContext(), todo.getNotificationId(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar notificationCalendar = Calendar.getInstance();
        notificationCalendar.set(todo.getNotifyYear(), todo.getNotifyMonth(), todo.getNotifyDay(), todo.getNotifyHour(), todo.getNotifyMinute(), 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationCalendar.getTimeInMillis(), broadcast);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationCalendar.getTimeInMillis(), broadcast);
        }
    }
}
