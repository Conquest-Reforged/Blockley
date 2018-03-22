const rateLimitMS = 400;
const allRows = JSON.parse(data);

var index = 0;
var pageCount = 1;
var rowsPerPage = 25;
var lock = false;
var searchFilter = (el, i) => true;

window.addEventListener("load", () => {
  console.log("initializing");
  var rowScroller = document.getElementById("rows-input");
  rowScroller.value = rowsPerPage;
  updatePage();
});

function previous(btn) {
  if (index > 0) {
    index--;
    updatePage();
  }
}

function next(btn) {
  if (index + 1 < pageCount) {
    index++;
    updatePage();
  }
}

function rows(input) {
  var contents = document.getElementById("contents");
  rowsPerPage = parseInt(input.value);
  updatePage();
}

function keyType(input) {
  if (lock) {
    return;
  }
  index = 0;
  setSearchTerm(input.value.toUpperCase());
  window.setTimeout(updatePage, rateLimitMS);
}

function setSearchTerm(text) {
  if (text === "") {
    searchFilter = e => true;
  } else {
    searchFilter = e => match(e.name, text) || match(e.id, text) || match(e.state, text);
  }
}

function updatePage() {
  lock = true;
  var rows = allRows.filter(searchFilter);
  pageCount = calcPageCount(rows.length);
  var start = index * rowsPerPage;
  var end = start + rowsPerPage;
  setContent(rows.slice(start, end));
  var page = document.getElementById("page-label");
  var pageNumber = index + 1;
  page.innerText = `Page: ${pageNumber} / ${pageCount}`;
  lock = false;
}

function calcPageCount(rows) {
    var div = rows / rowsPerPage;
    var round = Math.floor(div);
    if (div === round) {
        return round;
    }
    return 1 + round;
}

function setContent(rows) {
  var contents = document.getElementById("contents");
  var body = document.createElement("tbody");
  body.id = "contents";

  rows.forEach(e => {
    var row = createRow(e);
    body.appendChild(row);
  });

  contents.parentNode.replaceChild(body, contents);
}

function match(value, match) {
  return value.toUpperCase().indexOf(match) > -1;
}

function createRow(entry) {
  var img = document.createElement('img');
  img.src = `./images/${entry.identifier}.png`;
  img.onerror = function() {
    this.style.opacity = 0;
  };

  var icon = document.createElement('td');
  icon.appendChild(img);

  var name = document.createElement('td');
  name.innerText = entry.name;

  var id = document.createElement('td');
  id.innerText = entry.id;

  var state = document.createElement('td');
  state.innerText = entry.state;

  var row = document.createElement('tr');
  row.appendChild(icon);
  row.appendChild(name);
  row.appendChild(id);
  row.appendChild(state);

  return row;
}