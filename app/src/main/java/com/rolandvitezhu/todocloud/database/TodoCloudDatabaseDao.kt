package com.rolandvitezhu.todocloud.database

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.data.List
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.data.User
import com.rolandvitezhu.todocloud.helper.DateTimeRange
import com.rolandvitezhu.todocloud.helper.GeneralHelper.getCategoriesArrayList
import com.rolandvitezhu.todocloud.helper.GeneralHelper.getListsArrayList
import com.rolandvitezhu.todocloud.helper.GeneralHelper.getTodosArrayList
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.*
import java.util.concurrent.CompletableFuture

@Dao
interface TodoCloudDatabaseDao {

    @Insert
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM user ORDER BY _id DESC LIMIT 1")
    suspend fun getCurrentUser(): User?

    @Query("SELECT user_online_id FROM user ORDER BY _id DESC LIMIT 1")
    suspend fun getCurrentUserOnlineId(): String?

    @Query("SELECT api_key FROM user ORDER BY _id DESC LIMIT 1")
    suspend fun getCurrentApiKey(): String?

    @Insert
    suspend fun insertTodoInner(todo: Todo): Long

    suspend fun insertTodo(todo: Todo): Long
    {
        val _id = insertTodoInner(todo)
        fixTodoPositions()

        return _id
    }

    suspend fun updateTodo(todo: Todo) {
        if (todo._id != null && todo._id!! > 0) {
            // The Todo has been modified offline, therefore todo_online_id is null in the local
            // database yet
            updateTodoByIdInner(todo)
        } else {
            // The Todo has been modified online, therefore _id is unknown yet
            updateTodoByTodoOnlineIdInner(todo._id, todo.userOnlineId, todo.listOnlineId,
                    todo.title, todo.priority, todo.dueDate, todo.reminderDateTime, todo.description,
                    todo.completed, todo.rowVersion, todo.deleted, todo.dirty, todo.position,
                    todo.todoOnlineId)
        }
    }

    suspend fun getTodos(wherePrefix: String): ArrayList<Todo> {
        val standardWherePostfix: String = prepareStandardWherePostfix()
        val where = wherePrefix + standardWherePostfix

        val queryStringBuilder = StringBuilder(
                "SELECT " +
                        "   * " +
                        "FROM " +
                        "   todo " +
                        "WHERE " +
                        where + " " +
                        "ORDER BY " +
                        "   position;")

        val query = SimpleSQLiteQuery(queryStringBuilder.toString())

        return getTodosArrayList(getTodosInner(query))
    }

    suspend fun getTodosByWhereCondition(where: String?): ArrayList<Todo> {
        val queryStringBuilder = StringBuilder(
                "SELECT " +
                        "  * " +
                        "FROM " +
                        "   todo " +
                        "WHERE " +
                        (where ?: "") + " " +
                        "ORDER BY " +
                        "   position;")

        val query = SimpleSQLiteQuery(queryStringBuilder.toString())

        return getTodosArrayList(getTodosByWhereConditionInner(query))
    }

    @Query("SELECT " +
            "   positions.position " +
            "FROM " +
            "   (SELECT " +
            "       position, " +
            "       COUNT(*) c " +
            "   FROM " +
            "       todo " +
            "   GROUP BY " +
            "       position " +
            "   HAVING " +
            "       c > 1 " +
            "   ORDER BY " +
            "       position) AS positions")
    suspend fun getDuplicatePositionValuesForTodos(): kotlin.collections.List<Double>

    /**
     * Get the ids for the duplicated positions.
     * return A HashMap that has positions as keys and _ids as values.
     */
    suspend fun get_IdForDuplicatePositionValues(): HashMap<Double, kotlin.collections.List<Int>>? {
        val duplicatePositionValuesForTodos = getDuplicatePositionValuesForTodos()
        val _IdForDuplicatePositionValues = HashMap<Double, kotlin.collections.List<Int>>()
        for (nextPositionValue in duplicatePositionValuesForTodos) {
            _IdForDuplicatePositionValues[nextPositionValue] =
                    get_IdForDuplicatePositionValuesInner(nextPositionValue)
        }

        return _IdForDuplicatePositionValues
    }

    /**
     * Get the next first todo position from the database. If the local todo table is empty, this
     * value will be half of the biggest double number. Otherwise this position value will be in
     * between the smallest position value in the todo table and the smallest double value.
     * @return
     */
    suspend fun getNextFirstTodoPosition(): Double {
        val firstTodoPosition = getFirstTodoPosition()
        return if (firstTodoPosition != null)
        {
            getPositionBetween(1.0, firstTodoPosition)
        }
        else
        {
            Double.MAX_VALUE / 2
        }
    }

    /**
     * An interval/range for today in long representation LocalDateTime
     * @return
     */
    fun today(): DateTimeRange {
        val startOfToday: LocalDateTime
        val endOfToday: LocalDateTime
        val zdtStartOfToday: ZonedDateTime
        val zdtEndOfToday: ZonedDateTime

        val today: LocalDate = LocalDate.now()
        startOfToday = today.atStartOfDay()
        endOfToday = startOfToday.plusDays(1).minusNanos(1)
        zdtStartOfToday = startOfToday.atZone(ZoneId.systemDefault())
        zdtEndOfToday = endOfToday.atZone(ZoneId.systemDefault())

        return DateTimeRange(
                zdtStartOfToday.toInstant().toEpochMilli(),
                zdtEndOfToday.toInstant().toEpochMilli()
        )
    }

    /**
     * An interval/range for the next 7 days in long representation LocalDateTime
     * @return
     */
    fun next7Days(): DateTimeRange {
        val startOf7Days: LocalDateTime
        val endOf7Days: LocalDateTime
        val zdtStartOf7Days: ZonedDateTime
        val zdtEndOf7Days: ZonedDateTime

        val today: LocalDate = LocalDate.now()
        startOf7Days = today.atStartOfDay()
        endOf7Days = startOf7Days.plusDays(8).minusNanos(1)
        zdtStartOf7Days = startOf7Days.atZone(ZoneId.systemDefault())
        zdtEndOf7Days = endOf7Days.atZone(ZoneId.systemDefault())

        return DateTimeRange(
                zdtStartOf7Days.toInstant().toEpochMilli(),
                zdtEndOf7Days.toInstant().toEpochMilli()
        )
    }

    fun prepareTodayPredefinedListWherePrefix(): String {
        val todayDateTimeRange = today()
        return ("   due_date "
                + "BETWEEN "
                    + todayDateTimeRange.startOfRangeLong + " AND " +
                    + todayDateTimeRange.endOfRangeLong + " ")
    }

    suspend fun prepareTodayPredefinedListWhere(): String {
        val todayPredefinedListWherePrefix = prepareTodayPredefinedListWherePrefix()
        val standardWherePostfix = prepareStandardWherePostfix()
        return (todayPredefinedListWherePrefix + standardWherePostfix)
    }

    fun prepareNext7DaysPredefinedListWherePrefix(): String {
        val next7DaysDateTimeRange = next7Days()
        return ("   due_date "
                + "BETWEEN "
                    + next7DaysDateTimeRange.startOfRangeLong + " AND "
                + next7DaysDateTimeRange.endOfRangeLong + " ")
    }

    suspend fun prepareNext7DaysPredefinedListWhere(): String {
        val next7DaysPredefinedListWherePrefix = prepareNext7DaysPredefinedListWherePrefix()
        val standardWherePostfix = prepareStandardWherePostfix()
        return (next7DaysPredefinedListWherePrefix + standardWherePostfix)
    }

    suspend fun prepareAllPredefinedListWhere(): String =
            "completed = 0 AND " +
            "user_online_id = '" + getCurrentUserOnlineId() + "' AND " +
            "deleted = 0"

    suspend fun prepareCompletedPredefinedListWhere(): String =
            "completed = 1 AND " +
            "user_online_id = '" + getCurrentUserOnlineId() + "' AND " +
            "deleted = 0"

    suspend fun prepareSearchWhereCondition(queryText: String): String =
            prepareSearchWherePrefix(queryText) + prepareStandardWherePostfix()

    suspend fun getTodosWithReminder(): ArrayList<Todo> {
        val where = "reminder_date_time IS NOT NULL AND " +
                           "reminder_date_time <> ''"
        return getTodos(where)
    }

    fun getTodosWithReminderJava(): CompletableFuture<ArrayList<Todo>> =
        GlobalScope.future {
            val where = "reminder_date_time IS NOT NULL AND " +
                    "reminder_date_time <> ''"
            getTodos(where)
        }

    suspend fun getTodosByListOnlineId(listOnlineId: String): ArrayList<Todo> {
        val where = "list_online_id = '$listOnlineId'"
        return getTodos(where)
    }

    @Query("SELECT " +
            "   todo_online_id " +
            "FROM " +
            "   todo " +
            "WHERE " +
            "   list_online_id = :list_online_id;")
    suspend fun getTodoOnlineIdsByListOnlineId(list_online_id: String?): kotlin.collections.List<String>

    suspend fun getTodosToUpdate(): ArrayList<Todo> =
            getTodosArrayList(getTodosToUpdateInner(getCurrentUserOnlineId()))

    suspend fun getTodosToInsert(): ArrayList<Todo> =
            getTodosArrayList(getTodosToInsertInner(getCurrentUserOnlineId()))

    suspend fun isTodoExists(todoOnlineId: String): Boolean =
            !getTodoOnlineIdByTodoOnlineId(todoOnlineId).isNullOrBlank()

    suspend fun getLastTodoRowVersion(): Int = getLastTodoRowVersionInner() ?: 0

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   todo " +
            "WHERE " +
            "   todo_online_id = :todo_online_id;")
    suspend fun getTodo(todo_online_id: String?): Todo?

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   todo " +
            "WHERE " +
            "   _id = :id;")
    suspend fun getTodo(id: Long?): Todo?

    suspend fun softDeleteTodo(todoOnlineId: String) {
        softDeleteTodoInner(todoOnlineId)
        fixTodoPositions()
    }

    suspend fun softDeleteTodo(todo: Todo) {
        todo.todoOnlineId?.let { softDeleteTodo(it) }
    }

    /**
     * @return the created List's _id, if List created successfully, -1 otherwise.
     */
    @Insert
    suspend fun insertList(list: List): Long

    suspend fun updateList(list: List) {
        return if (list._id != null && list._id != 0L) {
            // The List has been modified offline, therefore list_online_id is null in the local
            // database yet
            updateListInner(list)
        } else {
            // The List has been modified online, therefore _id is unknown yet
            updateListByListOnlineId(list._id, list.userOnlineId, list.categoryOnlineId,
                    list.title, list.rowVersion, list.deleted, list.dirty, list.position,
                    list.listOnlineId)
        }
    }

    @Query("UPDATE " +
            "   list " +
            "SET " +
            "   deleted = 1, " +
            "   dirty = 1 " +
            "WHERE " +
            "   list_online_id = :list_online_id;")
    suspend fun softDeleteList(list_online_id: String?)

    suspend fun softDeleteListAndRelatedTodos(listOnlineId: String?) {
        val todoOnlineIds = getTodoOnlineIdsByListOnlineId(listOnlineId)
        val areRelatedTodos = todoOnlineIds.isNotEmpty()
        if (areRelatedTodos) {
            for (todoOnlineId in todoOnlineIds) {
                softDeleteTodo(todoOnlineId)
            }
        }
        softDeleteList(listOnlineId)
    }

    suspend fun getListsNotInCategory(): ArrayList<List> =
            getListsArrayList(getListsNotInCategoryInner(getCurrentUserOnlineId()))

    suspend fun getListsByCategoryOnlineId(categoryOnlineId: String): ArrayList<List> =
            getListsArrayList(
                    getListsByCategoryOnlineIdInner(getCurrentUserOnlineId(), categoryOnlineId))

    suspend fun isListExists(listOnlineId: String): Boolean =
            !getListOnlineIdByListOnlineId(listOnlineId).isNullOrBlank()

    suspend fun getLastListRowVersion(): Int = getLastListRowVersionInner() ?: 0

    suspend fun getListsToUpdate(): ArrayList<List> =
            getListsArrayList(getListsToUpdateInner(getCurrentUserOnlineId()))

    suspend fun getListsToInsert(): ArrayList<List> =
            getListsArrayList(getListsToInsertInner(getCurrentUserOnlineId()))

    /**
     * @return the created Category's _id, if Category created successfully, -1 otherwise.
     */
    @Insert
    suspend fun insertCategory(category: Category): Long

    @Query("UPDATE " +
            "   category " +
            "SET " +
            "   deleted = 1, " +
            "   dirty = 1 " +
            "WHERE " +
            "   category_online_id = :category_online_id;")
    suspend fun softDeleteCategory(category_online_id: String?)

    suspend fun softDeleteCategoryAndListsAndTodos(categoryOnlineId: String) {
        val lists = getListsByCategoryOnlineId(categoryOnlineId)
        val areRelatedLists = lists.isNotEmpty()
        if (areRelatedLists) {
            for ((_, listOnlineId) in lists) {
                softDeleteListAndRelatedTodos(listOnlineId)
            }
        }
        softDeleteCategory(categoryOnlineId)
    }

    suspend fun updateCategory(category: Category) {
        if (category._id != 0L) {
            // The Category has been modified offline, therefore category_online_id is null in the local
            // database yet
            updateCategoryInner(category)
        } else {
            // The Category has been modified online, therefore _id is unknown yet
            updateCategoryByCategoryOnlineId(category._id, category.userOnlineId,
                    category.title, category.rowVersion, category.deleted, category.dirty,
                    category.position, category.categoryOnlineId)
        }
    }

    suspend fun getCategoryByCategoryOnlineId(categoryOnlineId: String): Category? =
            getCategoryByCategoryOnlineIdInner(getCurrentUserOnlineId(), categoryOnlineId)

    suspend fun getCategories(): ArrayList<Category> =
            getCategoriesArrayList(getCategoriesInner(getCurrentUserOnlineId()))

    /**
     * Get all of the categories which are associated to the current user with all of the lists
     * which are related to these categories in a HashMap from the local database.
     * @return lhmCategories A HashMap containing all the categories and the related lists.
     */
    suspend fun getCategoriesAndLists(): LinkedHashMap<Category, kotlin.collections.List<List>> {
        val categories: kotlin.collections.List<Category> = getCategories()
        val lhmCategories = LinkedHashMap<Category, kotlin.collections.List<List>>()
        for (category in categories) {
            category.categoryOnlineId?.let {
                val listData: kotlin.collections.List<List> = getListsByCategoryOnlineId(it)
                lhmCategories[category] = listData
            }
        }

        return lhmCategories
    }

    suspend fun getCategoriesToUpdate(): ArrayList<Category> =
            getCategoriesArrayList(getCategoriesToUpdateInner(getCurrentUserOnlineId()))

    suspend fun getCategoriesToInsert(): ArrayList<Category> =
            getCategoriesArrayList(getCategoriesToInsertInner(getCurrentUserOnlineId()))

    suspend fun isCategoryExists(categoryOnlineId: String): Boolean =
            !getCategoryOnlineIdByCategoryOnlineId(categoryOnlineId).isNullOrBlank()

    suspend fun getLastCategoryRowVersion(): Int = getLastCategoryRowVersionInner() ?: 0

    @Query("SELECT " +
            "   row_version " +
            "FROM " +
            "   category " +
            "ORDER BY " +
            "   row_version DESC " +
            "LIMIT 1;")
    suspend fun getLastCategoryRowVersionInner(): Int?

    @Query("SELECT " +
            "   category_online_id " +
            "FROM " +
            "   category " +
            "WHERE " +
            "   category_online_id = :category_online_id;")
    suspend fun getCategoryOnlineIdByCategoryOnlineId(category_online_id: String?): String?

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   category " +
            "WHERE " +
            "   user_online_id = :user_online_id AND " +
            "   dirty = 1 AND " +
            "   row_version = 0;")
    suspend fun getCategoriesToInsertInner(user_online_id: String?): kotlin.collections.List<Category>

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   category " +
            "WHERE " +
            "   user_online_id = :user_online_id AND " +
            "   dirty = 1 AND " +
            "   row_version > 0;")
    suspend fun getCategoriesToUpdateInner(user_online_id: String?): kotlin.collections.List<Category>

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   category " +
            "WHERE " +
            "   user_online_id = :user_online_id AND " +
            "   deleted = 0 " +
            "ORDER BY " +
            "   title;")
    suspend fun getCategoriesInner(user_online_id: String?): kotlin.collections.List<Category>

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   category " +
            "WHERE " +
            "   user_online_id = :user_online_id AND " +
            "   deleted = 0 AND " +
            "   category_online_id = :category_online_id;")
    suspend fun getCategoryByCategoryOnlineIdInner(user_online_id: String?,
                                           category_online_id: String?): Category?

    @Update
    suspend fun updateCategoryInner(category: Category)
    
    @Query("UPDATE " +
            "   category " +
            "SET " +
            "   _id = :id, " +
            "   user_online_id = :user_online_id, " +
            "   title = :title, " +
            "   row_version = :row_version, " +
            "   deleted = :deleted, " +
            "   dirty = :dirty, " +
            "   position = :position " +
            "WHERE " +
            "   category_online_id = :category_online_id;")
    suspend fun updateCategoryByCategoryOnlineId(id: Long?, user_online_id: String?, title: String,
                                         row_version: Int, deleted: Boolean?, dirty: Boolean,
                                         position: Double, category_online_id: String?)

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   list " +
            "WHERE " +
            "   user_online_id = :user_online_id AND " +
            "   dirty = 1 AND " +
            "   row_version = 0;")
    suspend fun getListsToInsertInner(user_online_id: String?): kotlin.collections.List<List>

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   list " +
            "WHERE " +
            "   user_online_id = :user_online_id AND " +
            "   dirty = 1 AND " +
            "   row_version > 0;")
    suspend fun getListsToUpdateInner(user_online_id: String?): kotlin.collections.List<List>

    @Query("SELECT " +
            "   row_version " +
            "FROM " +
            "   list " +
            "ORDER BY " +
            "   row_version DESC " +
            "LIMIT 1;")
    suspend fun getLastListRowVersionInner(): Int?

    @Query("SELECT " +
            "   list_online_id " +
            "FROM " +
            "   list " +
            "WHERE " +
            "   list_online_id = :list_online_id;")
    suspend fun getListOnlineIdByListOnlineId(list_online_id: String?): String?

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   list " +
            "WHERE " +
            "   user_online_id = :user_online_id AND " +
            "   category_online_id = :category_online_id AND " +
            "   deleted = 0 " +
            "ORDER BY " +
            "   title;")
    suspend fun getListsByCategoryOnlineIdInner(user_online_id: String?,
                                        category_online_id: String?): kotlin.collections.List<List>

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   list " +
            "WHERE " +
            "   user_online_id = :user_online_id AND " +
            "   (category_online_id IS NULL OR category_online_id = '') AND " +
            "   deleted = 0 " +
            "ORDER BY " +
            "   title;")
    suspend fun getListsNotInCategoryInner(user_online_id: String?): kotlin.collections.List<List>

    @Query("UPDATE " +
            "   list " +
            "SET " +
            "   _id = :id, " +
            "   user_online_id = :user_online_id, " +
            "   category_online_id = :category_online_id, " +
            "   title = :title, " +
            "   row_version = :row_version, " +
            "   deleted = :deleted, " +
            "   dirty = :dirty, " +
            "   position = :position " +
            "WHERE " +
            "   list_online_id = :list_online_id;")
    suspend fun updateListByListOnlineId(id: Long?, user_online_id: String?, category_online_id: String?,
                                 title: String, row_version: Int, deleted: Boolean?, dirty: Boolean,
                                 position: Double, list_online_id: String?)

    @Update
    suspend fun updateListInner(list: List)

    @Query("UPDATE " +
            "   todo " +
            "SET " +
            "   deleted = 1, " +
            "   dirty = 1 " +
            "WHERE " +
            "   todo_online_id = :todo_online_id;")
    suspend fun softDeleteTodoInner(todo_online_id: String?)

    @Query("SELECT " +
            "   row_version " +
            "FROM " +
            "   todo " +
            "ORDER BY " +
            "   row_version DESC " +
            "LIMIT 1;")
    suspend fun getLastTodoRowVersionInner(): Int?

    @Query("SELECT " +
            "   todo_online_id " +
            "FROM " +
            "   todo " +
            "WHERE " +
            "   todo_online_id = :todo_online_id;")
    suspend fun getTodoOnlineIdByTodoOnlineId(todo_online_id: String?): String?

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   todo " +
            "WHERE " +
            "   user_online_id = :user_online_id AND " +
            "dirty = 1 AND " +
            "row_version = 0;")
    suspend fun getTodosToInsertInner(user_online_id: String?): kotlin.collections.List<Todo>

    @Query("SELECT " +
            "   * " +
            "FROM " +
            "   todo " +
            "WHERE " +
            "   user_online_id = :user_online_id AND " +
            "   dirty = 1 AND " +
            "   row_version > 0;")
    suspend fun getTodosToUpdateInner(user_online_id: String?): kotlin.collections.List<Todo>

    fun prepareSearchWherePrefix(queryText: String): String =
            "(title LIKE '%" + queryText + "%' OR " +
            "description LIKE '%" + queryText + "%')"

    @Query("SELECT " +
            "   position " +
            "FROM " +
            "   todo " +
            "ORDER BY " +
            "   position " +
            "LIMIT 1;")
    suspend fun getFirstTodoPosition(): Double?

    @Query("SELECT " +
            "   _id " +
            "FROM " +
            "   todo " +
            "WHERE " +
            "   position = :position " +
            "ORDER BY " +
            "   _id;")
    suspend fun get_IdForDuplicatePositionValuesInner(position: Double): kotlin.collections.List<Int>

    @RawQuery
    suspend fun getTodosByWhereConditionInner(query: SupportSQLiteQuery): kotlin.collections.List<Todo>

    suspend fun prepareStandardWherePostfix(): String =
            " AND " +
            "completed = 0 AND " +
            "user_online_id = '" + getCurrentUserOnlineId() + "' AND " +
            "deleted = 0 "

    @RawQuery
    suspend fun getTodosInner(query: SupportSQLiteQuery): kotlin.collections.List<Todo>

    @Update
    suspend fun updateTodoByIdInner(todo: Todo)

    @Query("UPDATE " +
            "   todo " +
            "SET " +
            "   _id = :id, " +
            "   user_online_id = :user_online_id, " +
            "   list_online_id = :list_online_id, " +
            "   title = :title, " +
            "   priority = :priority, " +
            "   due_date = :due_date, " +
            "   reminder_date_time = :reminder_date_time, " +
            "   description = :description, " +
            "   completed = :completed, " +
            "   row_version = :row_version, " +
            "   deleted = :deleted, " +
            "   dirty = :dirty, " +
            "   position = :position " +
            "WHERE " +
            "   todo_online_id = :todo_online_id;")
    suspend fun updateTodoByTodoOnlineIdInner(id: Long?, user_online_id: String?, list_online_id: String?,
                                      title: String?, priority: Boolean?, due_date: Long,
                                      reminder_date_time: Long, description: String?,
                                      completed: Boolean?, row_version: Int, deleted: Boolean?,
                                      dirty: Boolean, position: Double, todo_online_id: String?)

    /**
     * Fix the duplicated position field value of the all the todos in the database. We use this
     * method to prevent the duplication of the position values when using multiple devices
     * simultaneously. The fixed position value will be right between the previous and the next
     * position.
     */
    suspend fun fixTodoPositions() {
        var duplicatePositionTodos: kotlin.collections.MutableList<Todo> = mutableListOf()
        duplicatePositionTodos.addAll(getDuplicatePositionTodosForASinglePosition())
        while (duplicatePositionTodos.isNotEmpty()) {
            for (i in 0 until duplicatePositionTodos.size - 1) {
                val todoToBeFixed = duplicatePositionTodos[i + 1]
                val previousPosition = duplicatePositionTodos[i].position
                val nextPosition: Double = getNextTodoPosition(todoToBeFixed.position)
                fixTodoPosition(todoToBeFixed._id, previousPosition, nextPosition)
            }
            duplicatePositionTodos.clear()
            duplicatePositionTodos.addAll(getDuplicatePositionTodosForASinglePosition())
        }
    }

    @Query("SELECT " +
            "   t.* " +
            "FROM " +
            "   todo t " +
            "JOIN " +
            "   (SELECT " +
            "       position " +
            "   FROM " +
            "       todo " +
            "   GROUP BY " +
            "       position " +
            "   HAVING " +
            "       COUNT(*) > 1 " +
            "   LIMIT 1 " +
            "   ) p " +
            "   ON t.position = p.position " +
            "   ORDER BY " +
            "       row_version;")
    suspend fun getDuplicatePositionTodosForASinglePosition(): kotlin.collections.List<Todo>

    suspend fun getNextTodoPosition(position: Double): Double {
        var nextTodoPosition = getNextTodoPositionInner(position)
        if (nextTodoPosition == null)
            nextTodoPosition = getPositionBetween(position, Double.MAX_VALUE)

        return nextTodoPosition
    }

    /**
     * Get the position value of the next todo from the local database. If the position parameter
     * has the value of the last todo position, the returned position value will be between the last
     * todo position value and the biggest double value.
     * @param position The previous todo position value.
     * @return The next todo position value.
     */
    @Query("SELECT " +
            "   position " +
            "FROM " +
            "   todo " +
            "WHERE " +
            "   position > :position " +
            "ORDER BY " +
            "   position ASC " +
            "LIMIT 1;")
    suspend fun getNextTodoPositionInner(position: Double): Double?

    /**
     * Return a position value between the given position values.
     * @param previousPosition
     * @param nextPosition
     * @return
     */
    private fun getPositionBetween(previousPosition: Double, nextPosition: Double): Double =
            (nextPosition - previousPosition) / 2 + previousPosition

    /**
     * Fix the duplicated position field value of the todo in the database. We use this method to
     * prevent the duplication of the position values when using multiple devices simultaneously.
     * The fixed position value will be right between the previous and the next position.
     * @param todoToBeFixed The todo which has the duplicated position value.
     * @param previousPosition The position of the todo which is right before the todo which's
     * position should be fixed.
     * @param nextPosition The position of the todo which is right after the todo which's
     * position should be fixed.
     */
    @Query("UPDATE " +
            "   todo " +
            "SET " +
            "   position = :fixedPosition, " +
            "   dirty = 1 " +
            "WHERE " +
            "   _id = :id;")
    suspend fun fixTodoPositionInner(id: Long?, fixedPosition: Double)

    /**
     * Fix the duplicated position field value of the todo in the database. We use this method to
     * prevent the duplication of the position values when using multiple devices simultaneously.
     * The fixed position value will be right between the previous and the next position.
     * @param todoToBeFixed The todo which has the duplicated position value.
     * @param previousPosition The position of the todo which is right before the todo which's
     * position should be fixed.
     * @param nextPosition The position of the todo which is right after the todo which's
     * position should be fixed.
     */
    suspend fun fixTodoPosition(_id: Long?, previousPosition: Double, nextPosition: Double) {
        if (_id != null) {
            val fixedPosition = getPositionBetween(previousPosition, nextPosition)
            fixTodoPositionInner(_id, fixedPosition)
        }
    }
}