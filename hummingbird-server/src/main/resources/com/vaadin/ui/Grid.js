module.addRow = function(grid, rowData) {
	if (!grid.data.source) {
		grid.data.source = [];
	}
	grid.data.source.push(rowData);

	// Workaround to make vaadin-grid notice changes in data
	grid.data.source = grid.data.source;
}

module.setRows = function(grid, rows) {
	grid.data.source = rows;
}