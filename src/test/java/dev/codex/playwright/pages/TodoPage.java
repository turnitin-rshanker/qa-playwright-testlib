package dev.codex.playwright.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import dev.codex.playwright.Base;
import dev.codex.playwright.locators.LocatorRef;
import dev.codex.playwright.locators.LocatorRefs;
import java.util.List;

public final class TodoPage extends Base {
  private static final LocatorRef TASK_INPUT = LocatorRefs.label("Add a task");
  private static final LocatorRef ADD_BUTTON = LocatorRefs.role(
      AriaRole.BUTTON,
      new Page.GetByRoleOptions().setName("Add").setExact(true));
  private static final LocatorRef TASK_ITEMS = LocatorRefs.selector("li");
  private static final LocatorRef COMPLETED_TASKS = LocatorRefs.selector("li.done");
  private static final LocatorRef TASK_NAMES = LocatorRefs.selector(".todo-text");

  private static final String APP = """
      <!doctype html>
      <html lang="en">
        <head>
          <meta charset="utf-8">
          <title>Local Todo App</title>
          <style>
            body { font: 16px system-ui; max-width: 42rem; margin: 3rem auto; }
            form { display: flex; gap: .5rem; }
            input[type=text] { flex: 1; padding: .6rem; }
            li { display: flex; justify-content: space-between; margin: .6rem 0; }
            li.done .todo-text { text-decoration: line-through; opacity: .6; }
          </style>
        </head>
        <body>
          <main>
            <h1>Tasks</h1>
            <form aria-label="Add task form">
              <label for="task">Add a task</label>
              <input id="task" type="text" required>
              <button type="submit">Add</button>
            </form>
            <ul aria-label="Task list"></ul>
          </main>
          <script>
            const form = document.querySelector('form');
            const input = document.querySelector('#task');
            const list = document.querySelector('ul');
            form.addEventListener('submit', event => {
              event.preventDefault();
              const task = input.value.trim();
              if (!task) return;
              const item = document.createElement('li');
              item.innerHTML = '<label><input type="checkbox"><span class="todo-text"></span></label><button type="button">Delete</button>';
              item.querySelector('.todo-text').textContent = task;
              const checkbox = item.querySelector('input');
              const remove = item.querySelector('button');
              checkbox.setAttribute('aria-label', 'Complete ' + task);
              remove.setAttribute('aria-label', 'Delete ' + task);
              checkbox.addEventListener('change', () => item.classList.toggle('done', checkbox.checked));
              remove.addEventListener('click', () => item.remove());
              list.appendChild(item);
              console.info('todo-added', task);
              input.value = '';
            });
          </script>
        </body>
      </html>
      """;

  public TodoPage(Page page) {
    super(page);
  }

  public TodoPage open() {
    actions().setContent(APP);
    waitUntilLoaded();
    return this;
  }

  @Override
  protected LocatorRef loadableSelector() {
    return TASK_INPUT;
  }

  public TodoPage addTask(String task) {
    setValue(TASK_INPUT, task).click(ADD_BUTTON);
    return this;
  }

  public TodoPage completeTask(String task) {
    check(LocatorRefs.role(
        AriaRole.CHECKBOX,
        new Page.GetByRoleOptions().setName("Complete " + task).setExact(true)));
    return this;
  }

  public TodoPage deleteTask(String task) {
    click(LocatorRefs.role(
        AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Delete " + task).setExact(true)));
    return this;
  }

  public int taskCount() {
    return getElementCount(TASK_ITEMS);
  }

  public int completedTaskCount() {
    return getElementCount(COMPLETED_TASKS);
  }

  public List<String> taskNames() {
    return getAllText(TASK_NAMES);
  }
}
