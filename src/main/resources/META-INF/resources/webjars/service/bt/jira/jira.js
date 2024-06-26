define(function () {
	const current = {

		/**
		 * Interval identifier for the refresh
		 */
		intervalVariable: null,

		btPriorities: {
			BLOCKER: 'ban text-danger',
			CRITICAL: 'exclamation text-danger',
			MAJOR: 'sort-up text-danger',
			MINOR: 'sort-down text-info',
			TRIVIAL: 'sort-down text-success'
		},

		subscription: null,
		
		renderTool: function() {
			_('importPopup').on('submit', function (e) {
				e.preventDefault();
				current.upload();
				return false;
			}).on('show.bs.modal', function (event) {
				// Reset the form but previous values
				validationManager.mapping.DEFAULT = 'csv-file';
				_('csv-upload-step-last').find('i').addClass('hide');
				let $source = $(event.relatedTarget);
				let uc = $source && current.$super('subscriptions').fnGetData($source.closest('tr')[0]);
				current.subscription = uc.id;
				current.resetUploadError();
			});
		},

		configureSubscriptionParameters: function (configuration) {
			current.registerJiraProjectSelect2(configuration, 'service:bt:jira:pkey');
		},

		/**
		 * Render Bug tracking JIRA data.
		 */
		renderFeatures: function (subscription) {
			let result = '';
			let params = subscription.parameters;
			result += current.$super('renderServiceLink')('home', params['service:bt:jira:url'] + '/browse/' + params['service:bt:jira:pkey'], 'service:bt:jira:url-pkey', undefined, ' target="_blank"');

			// Add export menu
			result += current.renderExportGroup(subscription);

			// Add import link
			result += current.$super('renderServiceLink')('upload', '#importPopup', 'service:bt:jira:import', undefined, ' class="dropdown-toggle" role="button" data-toggle="modal"');

			// Help
			result += current.$super('renderServiceHelpLink')(params, 'service:bt:help');
			return result;
		},

		/**
		 * Render JIRA project key.
		 */
		renderKey: function (subscription) {
			return current.$super('renderKey')(subscription, 'service:bt:jira:pkey');
		},

		renderExportGroup: function (subscription) {
			const now = moment();
			const linkRoot = REST_PATH + 'service/bt/jira/' + subscription.id + '/' + subscription.parameters['service:bt:jira:pkey'] + '-' + subscription.id + '-' + now.format('YYYY-MM-DD');
			const linkCsvSlaCf = current.$super('renderServiceLink')('file-text-o menu-icon', linkRoot + '-full.csv', undefined, 'service:bt:jira:sla-csv-full', ' download');
			const linkCsvSlaShort = current.$super('renderServiceLink')('file-text-o menu-icon', linkRoot + '-short.csv', undefined, 'service:bt:jira:sla-csv', ' download');
			const linkCsvStatus = current.$super('renderServiceLink')('file-text-o menu-icon', linkRoot + '-status.csv', undefined, 'service:bt:jira:sla-csv-status', ' download');
			const linkCsv = current.$super('renderServiceLink')('file-text-o menu-icon', linkRoot + '-simple.csv', undefined, 'service:bt:jira:csv', ' download');
			const linkXmlSla = current.$super('renderServiceLink')('file-excel-o menu-icon', linkRoot + '.xml', undefined, 'service:bt:jira:sla-xls', ' download');
			return '<div class="btn-group btn-link feature" data-container="body" data-toggle="tooltip" title="' + current.$messages['export'] + '"><i class="fas fa-download" data-toggle="dropdown"></i>'
			 + '<ul class="dropdown-menu dropdown-menu-right"><li>' + linkCsvSlaShort + '</li><li>' + linkCsvSlaCf + '</li><li>' + linkCsv + '</li><li>' + linkCsvStatus + '</li><li>' + linkXmlSla + '</li></ul></div>';
		},

		upload: function () {
			current.resetUploadError();
			_('importPopup').ajaxSubmit({
				url: REST_PATH + 'service/bt/jira/' + current.subscription + '/' + _('csv-upload-mode').val() + '/' + _('csv-upload-encoding').val(),
				type: 'POST',
				dataType: 'json',
				success: function (data) {
					current.unscheduleUploadStep();
					current.displayUploadResult(data);
					notifyManager.notify(Handlebars.compile(current.$messages['import-succeed'])(data.changes));
				},
				error: function () {
					current.unscheduleUploadStep();
					current.onUploadFailure();
					notifyManager.notifyDanger(current.$messages['import-failed']);
				}
			});

			current.scheduleUploadStep();
		},

		resetUploadError: function () {
			const $container = _('importPopup').find('.form-horizontal');
			$container.find('.step').remove();
			$container.find('.alert.static').addClass('hide').removeClass('in');
			validationManager.reset($('#importPopup'));
		},

		unscheduleUploadStep: function () {
			clearInterval(current.intervalVariable);
		},

		scheduleUploadStep: function () {
			current.intervalVariable = setInterval(function () {
				current.synchronizeUploadStep();
			}, 1000);
		},

		synchronizeUploadStep: function () {
			current.unscheduleUploadStep();
			$.ajax({
				dataType: 'json',
				url: REST_PATH + 'service/bt/jira/' + current.subscription + '/task',
				type: 'GET',
				success: function (data) {
					current.displayUploadResult(data);
					if (data.end) {
						return;
					}
					current.scheduleUploadStep();
				}
			});
		},

		onUploadFailure: function () {
			$.ajax({
				dataType: 'json',
				url: REST_PATH + 'service/bt/jira/' + current.subscription + '/task',
				type: 'GET',
				success: function (data) {
					current.displayUploadResult(data);
				}
			});
		},

		/**
		 * Update the upload result. The history is shorten to maximum 4 items.
		 */
		displayUploadResult: function (data) {
			const $container = _('importPopup').find('.form-horizontal');
			_('csv-upload-step-last').find('i').addClass('hide');
			$container.find('.step').remove();
			const lastIndex = data.step;
			const firstIndex = data.step > 4 ? data.step - 3 : 1;
			let i;
			if (firstIndex > 1) {
				$container.append('<div class="form-group step"><label class="control-label"><i class="fas fa-check" title="succeed"></i></label><div class="toggle-visibility"><strong>[...]</strong></div></div>');
				const $moreContainer = $container.find('.toggle-visibility');
				for (i = 1; i < firstIndex; i++) {
					$moreContainer.append('<div class="toggle-visibility-content">' + current.$messages.steps[i - 1] + '</div>');
				}
			}
			for (i = firstIndex; i < lastIndex; i++) {
				$container.append('<div class="form-group step"><label class="control-label"><i class="fas fa-check" title="succeed"></i></label><div>' + current.$messages.steps[i - 1] + '</div></div>');
			}

			const errorField = _('csv-file').closest('.form-group').attr('data-error-property');
			const errorMessage = errorField && $('#csv-file').closest('.form-group').attr('title');

			// Last failure step
			if (data.failed || errorField) {
				if (data.failed) {
					$container.append('<div class="form-group step"><label class="control-label" id="csv-upload-step-error"><i class="fas fa-times" title="failed"></i></label><div>' + current.$messages.steps[data.step - 1]);
				}
				if (errorField) {
					$container.prepend(current.newAlert('<strong>' + errorField + '</strong> ' + errorMessage).addClass('step'));
					current.unscheduleUploadStep();
				}
			} else if (data.end) {
				$container.prepend($container.append('<div class="alert alert-success step"><button class="close" data-dismiss="alert" type="button">&times;</button>' + (_('csv-upload-mode').val() === 'full' ? 'Terminé' : 'Fichier valide') + '</div>'));
				current.unscheduleUploadStep();
				if (_('csv-upload-mode').val() === 'full') {
					current.displayUploadFullResult($container, data);
				}
			} else {
				// Indicated the running step
				$container.append('<div class="form-group step step-last"><label class="control-label" id="csv-upload-step-last"><i class="fas fa-sync-alt fa-spin"></i></label><div>' + current.$messages.steps[data.step - 1] + '</div></div>');
			}

			// Add the summary
			let summary = '<div class="alert alert-info step"><button class="close" data-dismiss="alert" type="button">&times;</button>Démarré ';
			summary += moment(data.start).format(formatManager.messages.shortdateMomentJs);
			if (data.end) {
				summary += ', terminé ' + moment(data.end).format(formatManager.messages.shortdateMomentJs) + ' (' + momentManager.duration(moment().valueOf() - data.start) + ')';
			} else {
				summary += ' ' + momentManager.duration(data.start - moment().valueOf());
			}
			if (data.pkey) {
				summary += '<br/>Projet : ' + data.pkey + ' (' + data.jira + ')';
			}
			if (data.changes) {
				summary += '<br/>Tickets : ' + data.changes + ' changements';
				if (data.issues) {
					summary += ' pour ' + data.issues + ' tickets';
				}
			}

			// Add dependencies
			summary += current.addRequiredDependenciesMessage(data);
			summary += current.addCompletedDependenciesMessage(data);
			summary += current.addNewDependenciesMessage(data);
			summary += current.addNewDependenciesMessage('</div>');
			$container.append(summary);
		},

		displayUploadFullResult: function ($container, data) {
			if (data.canSynchronizeJira === true) {
				if (data.scriptRunner === false) {
					current.showStaticAlert(_('scriptRunner').closest('.static'));
				} else if (data.synchronizedJira === false) {
					current.showStaticAlert(_('alert-jira-cache'));
				}
			} else {
				current.showStaticAlert(_('alert-jira-admin'));
			}
		},

		showStaticAlert: function (selector) {
			selector.removeClass('hide').addClass('in');
		},

		/**
		 * Add required dependencies summary
		 */
		addRequiredDependenciesMessage: function (data) {
			if (data.priorities || data.statuses || data.types || data.resolutions) {
				const dataAsArray = [];
				data.priorities && dataAsArray.push(data.priorities + ' priorités');
				data.statuses && dataAsArray.push(data.statuses + ' statuts');
				data.types && dataAsArray.push(data.types + ' types');
				data.resolutions && dataAsArray.push(data.resolutions + ' résolutions');
				data.users && dataAsArray.push(data.users + ' utilisateurs');
				data.customFields && dataAsArray.push(data.customFields + ' champs personnalisés');
				return '<br/><strong>Required dependencies</strong> ' + dataAsArray.join(', ');
			}
			return '';
		},

		/**
		 * Add new dependencies summary
		 */
		addNewDependenciesMessage: function (data) {
			if (data.newVersions || data.newComponents || data.newIssues || data.newLabels) {
				const dataAsArray = [];
				data.newComponents && dataAsArray.push(data.newComponents + ' composants');
				data.newVersions && dataAsArray.push(data.newVersions + ' versions');
				data.newIssues && dataAsArray.push(data.newIssues + ' issues');
				data.newLabels && dataAsArray.push(data.newLabels + ' labels');
				return '<br/><strong>New dependencies</strong> ' + dataAsArray.join(', ');
			}
			return '';
		},

		/**
		 * Add imported dependencies summary
		 */
		addCompletedDependenciesMessage: function (data) {
			if (data.components || data.versions || data.labels) {
				const dataAsArray = [];
				data.components && dataAsArray.push(data.components + ' components');
				data.versions && dataAsArray.push(data.versions + ' versions');
				data.labels && dataAsArray.push(data.labels + ' labels');
				return '<br/><strong>Imported dependencies</strong> ' + dataAsArray.join(', ');
			}
			return '';
		},

		/**
		 * Render JIRA details : id, name and unresolved issues with priorities
		 */
		renderDetailsKey: function (subscription) {
			return current.$super('generateCarousel')(subscription, [['project', current.renderKey(subscription)], ['name', subscription.data.project.description],
				['service:bt:sla:priorities', current.iconPriorities(subscription)]
			], 2);
		},

		/**
		 * Display the status pie chart
		 */
		renderDetailsFeatures: function (subscription, $tr, $td) {
			window.setTimeout(function () {
				current.pieStatuses(subscription, $td);
			}, 50);
			return '<span class="bt-jira-issues pie"></span>';
		},

		/**
		 * Render BT priorities icon and text
		 */
		iconPriorities: function (subscription) {
			let buffer = '';
			const params = subscription.parameters;
			const priorities = subscription.data.project.priorities;
			for (let priority in priorities) {
				if (priorities.hasOwnProperty(priority)) {
					if (buffer) {
						buffer += ' - ';
					}
					buffer += '<a target="blank" href="' + params['service:bt:jira:url'] + '/issues/?jql=project%20%3D%20' + params['service:bt:jira:pkey'] + '%20AND%20resolution%20%3D%20NULL%20AND%20priority%20%3D%20%22' + priority + '%22" data-container="#_ucDiv" data-toggle="tooltip" title="' + priority + '">' + current.$super('icon')(current.btPriorities[priority.toUpperCase()] || 'ellipsis-v text-danger') + priorities[priority] + '</a>';
				}
			}
			return buffer;
		},

		/**
		 * Render BT statuses pie chart keeping original order, and pushing low value to "other" category when
		 * there is not enough space.
		 */
		pieStatuses: function (subscription, $td) {
			let amount = 0;
			const filteredAmounts = [];
			const filteredStatuses = [];
			const min = {
				amount: 100000,
				index: 0
			};
			const params = subscription.parameters;
			const statuses = subscription.data.project.statuses;
			let total = 0;
			for (let status in statuses) {
				if (statuses.hasOwnProperty(status)) {
					amount = statuses[status];
					total += amount;
					current.addStatusToPie(status, amount, min, filteredAmounts, filteredStatuses);
				}
			}

			// Build the pie chart
			const $spark = $td.find('.bt-jira-issues');
			current.$super('sparklinePieZoom')($spark, filteredAmounts, {
				sliceColors: ['#EA632B', '#205081', '#D04437', '#A7A7A7'],
				zoomSize: $spark.closest('.masonry-container').length && '64px',
				tooltipFormatter: function (sparkline, options, fields) {
					if (fields.offset < 5) {
						return Handlebars.compile(current.$messages['service:bt:jira:status'])([fields.color, filteredStatuses[fields.offset], fields.value, total, current.$super('roundPercent')(fields.percent)]);
					}
					return Handlebars.compile(current.$messages['service:bt:jira:status'])([fields.color, filteredStatuses[fields.offset].join(', '), fields.value, total, current.$super('roundPercent')(fields.percent)]);
				},
				sparklineClick: function (ev) {
					const offset = ev.sparklines[0].getCurrentRegionFields().offset;
					if (typeof offset === 'undefined') {
						// Ignore this out of bound click
						return;
					}
					let multiple = '';
					let url = params['service:bt:jira:url'] + '/issues/?jql=project%20%3D%20' + params['service:bt:jira:pkey'] + '%20AND%20resolution%20%3D%20NULL%20AND%20';
					if (offset < 5) {
						// Single status
						url += 'status%20%3D%20%22' + filteredStatuses[offset] + '%22';
					} else if (offset) {
						// Multiple statuses, build "AND"
						for(let status of filteredStatuses[4]) {
							if (multiple) {
								multiple += '%20OR';
							} else {
								multiple += '(';
							}
							multiple += 'status%20%3D%20%22' + status;
						}
						if (multiple) {
							multiple += ')';
						}
						url += multiple;
					}
	
					// Open a new tag with the right filters
					window.open(url, '_blank')?.focus();
				}
			});
		},

		/**
		 * Add a status to pie chart data. Depending on the current values and the minimal pointer, the status will be placed in the other category, or will replace the current minimal status.
		 * @param {integer} status Status identifier.
		 * @param {integer} amount Amount of tickets for this status
		 * @param  {object} min    The current minimal pointer to update if the given values are lower.
		 * @param {[type]} filteredAmounts  Pie chart data of amounts of computed statues.
		 * @param {[type]} filteredStatuses  Pie chart data of index of computed statues.
		 */
		addStatusToPie: function (status, amount, min, filteredAmounts, filteredStatuses) {
			if (filteredStatuses.length < 4) {
				// Simply add the value, there is a place for this value
				current.updateMinStatus(amount, filteredAmounts.length, min);
				filteredStatuses.push(status);
				filteredAmounts.push(amount);
			} else {
				// Create the "other" zone as needed
				if (filteredStatuses.length === 4) {
					filteredStatuses.push([]);
					filteredAmounts.push(0);
				}

				if (amount <= min.amount) {
					// Put data directly in "other"
					filteredStatuses[4].push(status);
					filteredAmounts[4] += amount;
				} else {
					// Update the other amount with moved count
					filteredStatuses[4].push(filteredStatuses[min.index]);
					filteredAmounts[4] += filteredAmounts[min.index];
					filteredStatuses[min.index] = status;
					filteredAmounts[min.index] = amount;

					// Recompute the minimal values
					min.amount = 100000;
					for (let index = 0; index < 4; index++) {
						current.updateMinStatus(filteredAmounts[index], index, min);
					}
				}
			}
		},

		/**
		 * Update the minimal pie chart value from the given amount anf index.
		 * @param  {integer} amount Current amount of tickets for the current status.
		 * @param  {integer} index  Position of this status.
		 * @param  {object} min    The current minimal pointer to update if the given values are lower.
		 */
		updateMinStatus: function (amount, index, min) {
			if (amount <= min.amount) {
				min.index = index;
				min.amount = amount;
			}
		},

		/**
		 * Disable manual edition of "pkey" and "project" identifier parameters and add a Select2 server
		 * suggest filling the previous parameters on
		 * project selection.
		 */
		registerJiraProjectSelect2: function (configuration, id) {
			const cProviders = configuration.providers['form-group'];
			const previousProvider = cProviders[id] || cProviders.standard;
			cProviders[id] = function (parameter, container, $input) {
				// Disable computed parameters and remove the useless 'validate' button
				const $fieldset = previousProvider(parameter, container, $input).parent();
				_('service:bt:jira:pkey').attr('disabled', 'disabled');
				_('service:bt:jira:project').attr('disabled', 'disabled');

				// Create the select2 suggestion a LIKE %criteria% for project name and pkey
				const $select = $('<input class="form-control" type="text" id="service:bt:jira:pkey-project" autocomplete="off">');
				cProviders.standard({
					id: 'service:bt:jira:pkey-project'
				}, $fieldset, $select);
				current.$super('newNodeSelect2')($select, 'service/bt/jira/', current.$super('toDescription'), function (e) {
					// Fill the 'pkey' and 'project' parameters
					if (e.added) {
						_('service:bt:jira:pkey').val(e.added.name);
						_('service:bt:jira:project').val(e.added.id);
					}
				}, parameter, configuration.node + '/project/');
			};
		}
	};
	return current;
});
