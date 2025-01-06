/**
 * The JavaScript file handling UI interactions, charts, and file uploads
 */

// Wait for DOM to be fully loaded before initializing components
document.addEventListener('DOMContentLoaded', function() {
  // Initialize temporary alerts to auto-dismiss after 5 seconds
  const alerts = document.querySelectorAll('.alert')
  alerts.forEach(function(alert) {
    if (!alert.classList.contains('alert-permanent')) {
      setTimeout(function() {
        alert.classList.add('fade')
        setTimeout(function() {
          alert.remove()
        }, 150)  // Fade out duration
      }, 5000)   // Alert display duration
    }
  })

  // Initialize Bootstrap tooltips
  const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
  tooltipTriggerList.map(function(tooltipTriggerEl) {
    return new bootstrap.Tooltip(tooltipTriggerEl)
  })

  // Set up clipboard copy functionality for copy buttons
  document.querySelectorAll('.copy-button').forEach(button => {
    button.addEventListener('click', function() {
      const content = this.getAttribute('data-content')
      navigator.clipboard.writeText(content).then(() => {
        // Get tooltip instance and original text
        const tooltip = bootstrap.Tooltip.getInstance(this)
        const originalTitle = this.getAttribute('data-bs-original-title')

        // Show "Copied!" message
        tooltip.setContent({ '.tooltip-inner': 'הועתק!' })
        tooltip.show()

        // Reset tooltip text after delay
        setTimeout(() => {
          tooltip.setContent({ '.tooltip-inner': originalTitle })
        }, 1500)
      }).catch(err => {
        console.error('Failed to copy text: ', err)
      })
    })
  })

  // Initialize drag and drop zones for file uploads
  document.querySelectorAll('[data-upload-zone]').forEach(zone => {
    initializeDropZone(zone)
  })

  // Set up file input handlers and customize their appearance
  document.querySelectorAll('input[type="file"]').forEach(input => {
    input.addEventListener('change', handleFileSelect)
    customizeFileInputText(input)
  })

  // Initialize various charts if their data is available
  if (window.categoriesHierarchyData) {
    initializeCategoriesChart(window.categoriesHierarchyData)
  }

  if(window.miniCategoriesData && window.treeMapOptions){
    initializeTreeMap(window.miniCategoriesData, window.treeMapOptions)
  }

  initializeUsersChart(window.authorizedUsers, window.adminUsers, window.unauthorizedUsers)

  initUserActivityChart(window.userCountsByDate, window.cumulativeCountsByDate)
})

/**
 * Toggles between different modal view modes (view, edit, smart edit)
 * @param {HTMLElement} button - The button that triggered the mode change
 * @param {string} mode - The mode to switch to ('view', 'edit', or 'smart')
 */
function toggleMode(button, mode) {
  const modal = button.closest('.modal')
  const viewMode = modal.querySelector('.view-mode')
  const editMode = modal.querySelector('.edit-mode')
  const smartEditMode = modal.querySelector('.smart-edit-mode')
  const editButton = modal.querySelector('.edit-text')
  const smartEditButton = modal.querySelector('.smart-edit-text')

  // Hide all modes first
  viewMode.classList.add('d-none')
  editMode.classList.add('d-none')
  smartEditMode.classList.add('d-none')

  // Reset button texts to default
  editButton.textContent = 'ערוך'
  smartEditButton.textContent = 'עריכה חכמה'

  // Show selected mode and update button text
  switch (mode) {
    case 'view':
      viewMode.classList.remove('d-none')
      break
    case 'edit':
      editMode.classList.remove('d-none')
      editButton.textContent = 'תצוגה'
      break
    case 'smart':
      smartEditMode.classList.remove('d-none')
      smartEditButton.textContent = 'תצוגה'
      break
  }
}

/**
 * Sets up drag and drop functionality for a file upload zone
 * @param {HTMLElement} zone - The drop zone element to initialize
 */
function initializeDropZone(zone) {
  const input = zone.querySelector('input[type="file"]')
  if (!input) return;

  // Prevent browser default drag and drop behavior
  ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
    zone.addEventListener(eventName, preventDefaults, false)
  });

  // Add visual feedback when dragging over the zone
  ['dragenter', 'dragover'].forEach(eventName => {
    zone.addEventListener(eventName, () => {
      zone.classList.add('border-primary')
      zone.classList.add('border')
    })
  });

  // Remove highlighting when leaving drop zone
  ['dragleave', 'drop'].forEach(eventName => {
    zone.addEventListener(eventName, () => {
      zone.classList.remove('border-primary')
      zone.classList.remove('border')
    })
  })

  // Handle file drop
  zone.addEventListener('drop', (e) => {
    const dt = e.dataTransfer
    const files = dt.files

    // Update input files with dropped files
    const dataTransfer = new DataTransfer()
    Array.from(files).forEach(file => dataTransfer.items.add(file))
    input.files = dataTransfer.files

    handleFileSelect({ target: input })
  })
}

/**
 * Handles file selection events for file inputs
 * @param {Event} e - The file input change event
 */
function handleFileSelect(e) {
  const input = e.target
  const file = input.files[0]
  if (!file) return

  const dropZone = input.closest('[data-upload-zone]')
  const isImageInput = input.accept.includes('image')

  // Find preview elements
  const preview = dropZone.querySelector('.file-preview')
  const imagePreview = dropZone.querySelector('.image-preview')
  const fileName = dropZone.querySelector('.file-name')

  // Update filename display
  if (fileName) {
    fileName.textContent = file.name
  }

  // Handle image preview for image files
  if (isImageInput && file.type.startsWith('image/') && imagePreview) {
    const reader = new FileReader()
    reader.onload = function(e) {
      imagePreview.src = e.target.result
      imagePreview.classList.remove('d-none')
    }
    reader.readAsDataURL(file)
  }

  // Show preview container
  if (preview) {
    preview.classList.remove('d-none')
  }
}

/**
 * Prevents default browser behavior for events
 * @param {Event} e - The event to prevent defaults for
 */
function preventDefaults(e) {
  e.preventDefault()
  e.stopPropagation()
}

/**
 * Customizes the text displayed on file input labels
 * @param {HTMLInputElement} input - The file input element
 */
function customizeFileInputText(input) {
  const label = input.closest('[data-upload-zone]').querySelector('label')
  if (!label) return

  const isImageInput = input.accept.includes('image')
  label.textContent = isImageInput ? 'בחירת תמונה' : 'בחירת קובץ'
}

/**
 * Clears a file input and its associated preview elements
 * @param {string} inputId - The ID of the file input to clear
 */
function clearFileInput(inputId) {
  const input = document.getElementById(inputId)
  if (!input) return

  const dropZone = input.closest('[data-upload-zone]')
  if (!dropZone) return

  // Reset input and preview elements
  input.value = ''
  const preview = dropZone.querySelector('.file-preview')
  const imagePreview = dropZone.querySelector('.image-preview')
  const fileName = dropZone.querySelector('.file-name')

  if (preview) preview.classList.add('d-none')
  if (imagePreview) {
    imagePreview.classList.add('d-none')
    imagePreview.src = ''
  }
  if (fileName) fileName.textContent = ''
}

/**
 * Initializes the categories treemap chart
 * @param {Object} hierarchyData - Nested object containing category hierarchy data
 */
function initializeCategoriesChart(hierarchyData) {
  if (document.getElementById('categoriesChart') === null)
    return
  const ctx = document.getElementById('categoriesChart').getContext('2d')

  // Transform nested hierarchy into flat array for chart
  const transformedData = [];
  Object.entries(hierarchyData).forEach(([category, subCategories]) => {
    Object.entries(subCategories).forEach(([subCategory, count]) => {
      transformedData.push({
        category: category,
        subCategory: subCategory,
        value: count
      });
    });
  });

  // Color generator closure for consistent category colors
  const generateCategoryColors = (() => {
    const baseColors = [
      { r: 78, g: 121, b: 167 },   // Blue
      { r: 242, g: 142, b: 43 },   // Orange
      { r: 89, g: 161, b: 79 },    // Green
      { r: 237, g: 201, b: 72 },   // Yellow
      { r: 225, g: 87, b: 89 },    // Red
      { r: 130, g: 183, b: 180 },  // Teal
      { r: 176, g: 122, b: 161 },  // Purple
      { r: 255, g: 157, b: 167 },  // Pink
      { r: 156, g: 117, b: 95 }    // Brown
    ];

    let colorIndex = 0;
    const categoryColorMap = new Map();

    return (category) => {
      if (!categoryColorMap.has(category)) {
        const baseColor = baseColors[colorIndex % baseColors.length];
        const colors = [
          `rgb(${baseColor.r}, ${baseColor.g}, ${baseColor.b})`,
          `rgb(${Math.min(baseColor.r + 30, 255)}, ${Math.min(baseColor.g + 30, 255)}, ${Math.min(baseColor.b + 30, 255)})`
        ];
        categoryColorMap.set(category, colors);
        colorIndex++;
      }
      return categoryColorMap.get(category);
    };
  })();

  // Create treemap chart with configured options
  new Chart(ctx, {
    type: 'treemap',
    data: {
      datasets: [{
        tree: transformedData,
        key: 'value',
        groups: ['category', 'subCategory'],
        spacing: 1,
        borderWidth: 1.5,
        borderColor: 'white',
        backgroundColor: function(ctx) {
          if (!ctx.raw) return 'gray';
          const category = ctx.raw._data.category || 'לא מזוהה';
          const palette = generateCategoryColors(category);
          return palette[ctx.dataIndex % palette.length];
        },
        labels: {
          display: true,
          align: 'center',
          position: 'center',
          formatter: function(ctx) {
            if (!ctx.raw) return 'לא מזוהה';
            const subCategory = ctx.raw._data.subCategory;
            const value = ctx.raw._data.value;
            return `${subCategory}\n(${value})`;
          },
          font: {
            family: "'Heebo', sans-serif",
            size: 13
          },
          color: function(ctx) {
            const backgroundColor = ctx.dataset.backgroundColor(ctx);
            const rgb = backgroundColor.match(/\d+/g);
            if (!rgb) return '#000000';
            // Calculate brightness to determine text color
            const brightness = (parseInt(rgb[0]) * 299 + parseInt(rgb[1]) * 587 + parseInt(rgb[2]) * 114) / 1000;
            return brightness > 128 ? '#000000' : '#FFFFFF';
          }
        }
      }]
    },
    options: {
      plugins: {
        title: {
          display: true,
          text: 'התפלגות קטגוריות ותת קטגוריות לכמות הודעות',
          font: {
            size: 16,
            weight: 'bold'
          }
        },
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            title: function(context) {
              const item = context[0].raw;
              if (!item) return 'לא מזוהה';
              return `${item._data.category}`;
            },
            label: function(context) {
              return `כמות: ${context.raw._data.value}`;
            }
          }
        }
      }
    }
  });
}

/**
 * Initializes a simple treemap visualization
 * @param {Array} data - Array of data points for the treemap
 * @param {Object} options - Chart configuration options
 */
function initializeTreeMap(data, options) {
  if (document.getElementById('treeMapChart') === null)
    return
  const ctx = document.getElementById('treeMapChart').getContext('2d');

  // Create basic treemap with alternating colors
  new Chart(ctx, {
    type: 'treemap',
    data: {
      datasets: [{
        tree: data,
        key: 'value',
        groups: ['name'],
        spacing: 0.5,
        borderWidth: 1,
        borderColor: 'white',
        backgroundColor: function(ctx) {
          const colors = ['#FF9999', '#99FF99', '#9999FF', '#FFFF99'];
          return colors[ctx.dataIndex % colors.length];
        },
        labels: {
          display: true,
          align: 'center',
          position: 'center',
          font: {
            family: "'Heebo', sans-serif",
            size: 14
          }
        }
      }]
    },
    options: options
  });
}

/**
 * Initializes a pie chart showing the distribution of user authorization types
 *
 * @param {number} authorizedUsers - Count of regular authorized users
 * @param {number} adminUsers - Count of admin users
 * @param {number} unauthorizedUsers - Count of unauthorized users
 * @returns {void}
 */
function initializeUsersChart(authorizedUsers, adminUsers, unauthorizedUsers) {
  // Exit if chart container doesn't exist in DOM
  if (document.getElementById('authorizationChart') === null)
    return

  // Calculate total users for percentage calculations
  const total = authorizedUsers + adminUsers + unauthorizedUsers;

  // Get the canvas context for drawing
  const ctx = document.getElementById('authorizationChart').getContext('2d');

  // Create new Chart.js pie chart
  new Chart(ctx, {
    type: 'pie',
    data: {
      // Define labels for each user type segment
      labels: ['משתמשים מורשים', 'משתמשי מנהל', 'משתמשים לא מורשים'],
      datasets: [{
        // Map user counts to data array
        data: [authorizedUsers, adminUsers, unauthorizedUsers],
        // Define colors for each segment using Bootstrap theme colors
        backgroundColor: [
          '#198754',  // Success green for authorized users
          '#0dcaf0',  // Info blue for admin users
          '#212529'   // Dark gray for unauthorized users
        ],
        borderWidth: 1
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        // Configure legend display
        legend: {
          position: 'bottom',
          labels: {
            font: { size: 14 }
          }
        },
        // Configure tooltip to show count and percentage
        tooltip: {
          callbacks: {
            label: function(context) {
              const value = context.raw;
              const percentage = ((value / total) * 100).toFixed(1);
              return `${context.label}: ${value} (${percentage}%)`;
            }
          }
        },
        // Configure data labels shown directly on chart segments
        datalabels: {
          color: '#fff',
          font: {
            weight: 'bold',
            size: 18
          },
          // Only show labels for segments with > 0%
          formatter: function(value, context) {
            const percentage = ((value / total) * 100).toFixed(1);
            return percentage > 0 ? `${value}\n(${percentage}%)` : null;
          },
          anchor: 'center',
          align: 'right',
          offset: 20,
          padding: { left: 0, right: 0 }
        }
      },
      layout: {
        padding: { top: 10, bottom: 10 }
      }
    },
    // Enable Chart.js DataLabels plugin
    plugins: [ChartDataLabels]
  });
}

/**
 * Initializes a line chart showing user activity over time
 *
 * @param {Object} datesGlobal - Object containing dates as keys and daily new user counts as values
 * @param {Object} countsGlobal - Object containing dates as keys and cumulative user counts as values
 * @returns {void}
 */
function initUserActivityChart(datesGlobal, countsGlobal) {
  // Exit if chart container doesn't exist in DOM
  if (document.getElementById('usersActivityChart') === null)
    return

  const ctx = document.getElementById('usersActivityChart')

  // Extract arrays of dates and cumulative counts from input objects
  const dates = Object.keys(datesGlobal)
  const counts = Object.values(countsGlobal)

  // Create new Chart.js line chart
  new Chart(ctx, {
    type: 'line',
    data: {
      labels: dates,
      datasets: [{
        label: 'סה״כ משתמשים',
        data: counts,
        borderColor: '#25D366',  // WhatsApp green color
        backgroundColor: 'rgba(37, 211, 102, 0.1)',  // Transparent green
        fill: true  // Fill area under the line
      }]
    },
    options: {
      responsive: true,
      scales: {
        // Configure X axis as time scale
        x: {
          type: 'time',
          time: {
            unit: 'day',
            displayFormats: {
              day: 'yyyy-MM-dd'
            }
          },
          title: {
            display: true,
            text: 'תאריך',
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        },
        // Configure Y axis for user counts
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: 'מספר משתמשים',
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        }
      },
      plugins: {
        // Configure legend
        legend: {
          labels: {
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        },
        // Configure tooltip to show both total and new users
        tooltip: {
          titleFont: { size: 14 },
          bodyFont: { size: 16 },
          callbacks: {
            label: function(context) {
              const date = context.label
              const totalUsers = context.parsed.y
              // Get daily new users count from datesGlobal object
              const dailyUsers = window.userCountsByDate[date] || 0
              return [
                `סה״כ משתמשים: ${totalUsers}`,
                `משתמשים חדשים: ${dailyUsers}`
              ]
            }
          }
        }
      }
    }
  })
}