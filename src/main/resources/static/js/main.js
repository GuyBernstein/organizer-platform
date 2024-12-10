document.addEventListener('DOMContentLoaded', function() {
  const alerts = document.querySelectorAll('.alert')
  alerts.forEach(function(alert) {
    if (!alert.classList.contains('alert-permanent')) {
      setTimeout(function() {
        alert.classList.add('fade')
        setTimeout(function() {
          alert.remove()
        }, 150)
      }, 5000)
    }
  })

  // Initialize tooltips
  const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
  tooltipTriggerList.map(function(tooltipTriggerEl) {
    return new bootstrap.Tooltip(tooltipTriggerEl)
  })

  // Initialize copy buttons
  document.querySelectorAll('.copy-button').forEach(button => {
    button.addEventListener('click', function() {
      const content = this.getAttribute('data-content')
      navigator.clipboard.writeText(content).then(() => {
        // Create or update tooltip
        const tooltip = bootstrap.Tooltip.getInstance(this)
        const originalTitle = this.getAttribute('data-bs-original-title')

        // Update tooltip content
        tooltip.setContent({ '.tooltip-inner': 'הועתק!' })

        // Show the updated tooltip
        tooltip.show()

        // Reset tooltip after delay
        setTimeout(() => {
          tooltip.setContent({ '.tooltip-inner': originalTitle })
        }, 1500)
      }).catch(err => {
        console.error('Failed to copy text: ', err)
      })
    })
  })

  // Initialize all upload zones
  document.querySelectorAll('[data-upload-zone]').forEach(zone => {
    initializeDropZone(zone)
  })

  // Initialize file inputs
  document.querySelectorAll('input[type="file"]').forEach(input => {
    input.addEventListener('change', handleFileSelect)
    customizeFileInputText(input)
  })

  // Access the data through the global variable
  if (window.categoriesHierarchyData) {
    initializeCategoriesChart(window.categoriesHierarchyData)
  }

  // Initialize the chart when the document is ready
  if(window.miniCategoriesData && window.treeMapOptions){
    initializeTreeMap(window.miniCategoriesData, window.treeMapOptions)
  }


  initializeUsersChart(window.authorizedUsers , window.adminUsers , window.unauthorizedUsers)

  initUserActivityChart(window.userCountsByDate, window.cumulativeCountsByDate)
})

// toggle between view, edit and smart edit mode in the modal
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

  // Reset button texts
  editButton.textContent = 'ערוך'
  smartEditButton.textContent = 'עריכה חכמה'

  // Show selected mode
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

function initializeDropZone(zone) {
  const input = zone.querySelector('input[type="file"]')
  if (!input) return;

  // Prevent defaults for all drag events
  ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
    zone.addEventListener(eventName, preventDefaults, false)
  });

  // Add highlighting when dragging over
  ['dragenter', 'dragover'].forEach(eventName => {
    zone.addEventListener(eventName, () => {
      zone.classList.add('border-primary')
      zone.classList.add('border')
    })
  });

  // Remove highlighting when leaving or dropping
  ['dragleave', 'drop'].forEach(eventName => {
    zone.addEventListener(eventName, () => {
      zone.classList.remove('border-primary')
      zone.classList.remove('border')
    })
  })

  // Handle the actual drop
  zone.addEventListener('drop', (e) => {
    const dt = e.dataTransfer
    const files = dt.files

    // Update the input's files
    const dataTransfer = new DataTransfer()
    Array.from(files).forEach(file => dataTransfer.items.add(file))
    input.files = dataTransfer.files

    // Trigger the file select handler
    handleFileSelect({ target: input })
  })
}

function handleFileSelect(e) {
  const input = e.target
  const file = input.files[0]
  if (!file) return

  const dropZone = input.closest('[data-upload-zone]')
  const isImageInput = input.accept.includes('image')

  // Find preview elements within the specific drop zone
  const preview = dropZone.querySelector('.file-preview')
  const imagePreview = dropZone.querySelector('.image-preview')
  const fileName = dropZone.querySelector('.file-name')

  // Update filename if element exists
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

  // Show preview container if it exists
  if (preview) {
    preview.classList.remove('d-none')
  }
}

function preventDefaults(e) {
  e.preventDefault()
  e.stopPropagation()
}

function customizeFileInputText(input) {
  const label = input.closest('[data-upload-zone]').querySelector('label')
  if (!label) return

  const isImageInput = input.accept.includes('image')
  label.textContent = isImageInput ? 'בחירת תמונה' : 'בחירת קובץ'
}

function clearFileInput(inputId) {
  const input = document.getElementById(inputId)
  if (!input) return

  const dropZone = input.closest('[data-upload-zone]')
  if (!dropZone) return

  // Clear the input
  input.value = ''

  // Reset preview elements
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

function initializeCategoriesChart(hierarchyData) {
  if (document.getElementById('categoriesChart') === null)
    return
  const ctx = document.getElementById('categoriesChart').getContext('2d')

  function transformToTreemapData(data) {
    // Add padding to values to make boxes larger
    const result = []
    const scaleFactor = 2 // Increase this to make boxes bigger

    data.forEach((category, index) => {
      result.push({
        name: category.name,
        // Scale up the values to force larger boxes
        value: category.value * scaleFactor,
        group: category.name,
        groupIndex: index,
        // Store original value for display
        displayValue: category.value
      })

      category.children.forEach(subCategory => {
        result.push({
          name: subCategory.name,
          // Scale up the values to force larger boxes
          value: subCategory.value * scaleFactor,
          group: category.name,
          parent: category.name,
          groupIndex: index,
          // Store original value for display
          displayValue: subCategory.value
        })
      })
    })
    return result
  }

  const treeData = transformToTreemapData(hierarchyData)

  const colorSchemes = {
    base: [
      'rgba(69, 123, 157, 0.8)',
      'rgba(124, 152, 133, 0.8)',
      'rgba(146, 111, 91, 0.8)',
      'rgba(133, 87, 108, 0.8)',
      'rgba(106, 153, 153, 0.8)'
    ],
    hover: [
      'rgba(69, 123, 157, 1)',
      'rgba(124, 152, 133, 1)',
      'rgba(146, 111, 91, 1)',
      'rgba(133, 87, 108, 1)',
      'rgba(106, 153, 153, 1)'
    ]
  }

  new Chart(ctx, {
    type: 'treemap',
    data: {
      datasets: [{
        tree: treeData,
        key: 'value',
        groups: ['group'],
        spacing: 1, // Reduced spacing to maximize content area
        backgroundColor(context) {
          if (!context.raw) return 'transparent'
          const item = treeData[context.dataIndex]
          if (!item) return colorSchemes.base[0]
          const colorIndex = item.groupIndex % colorSchemes.base.length
          return item.parent ?
            colorSchemes.base[colorIndex] + '88' :
            colorSchemes.base[colorIndex]
        },
        borderWidth: 1,
        borderColor(context) {
          if (!context.raw) return 'transparent'
          const item = treeData[context.dataIndex]
          if (!item) return colorSchemes.hover[0]
          const colorIndex = item.groupIndex % colorSchemes.hover.length
          return colorSchemes.hover[colorIndex]
        },
        labels: {
          display: true,
          align: 'center',
          position: 'middle',
          formatter: (context) => {
            if (!context.raw) return ''
            const item = treeData[context.dataIndex]
            if (!item) return ''

            // Adjusted threshold for visibility
            if (context.raw.h < 40 || context.raw.w < 60) return ''

            return [
              item.name,
              `${item.displayValue} הודעות`  // Use original value for display
            ]
          },
          font: {
            size: 24,
            weight: 'bold'
          },
          color: 'black',
          padding: 4
        }
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false, // Allow chart to determine its own height
      plugins: {
        title: {
          display: true,
          text: 'התפלגות הודעות לפי קטגוריה',
          font: {
            size: 36,
            weight: 'bold'
          },
          padding: {
            top: 10,
            bottom: 20
          }
        },
        legend: {
          display: false
        },
        tooltip: {
          rtl: true,
          titleFont: {
            size: 24
          },
          bodyFont: {
            size: 20
          },
          padding: 8,
          callbacks: {
            title(items) {
              if (!items[0] || !items[0].raw) return ''
              const item = treeData[items[0].dataIndex]
              return item ? item.name : ''
            },
            label(context) {
              if (!context.raw) return ''
              const item = treeData[context.dataIndex]
              return `מספר הודעות: ${item.displayValue}`
            }
          }
        }
      }
    }
  })
}


// Function to initialize the treemap
function initializeTreeMap(data, options) {
  if (document.getElementById('treeMapChart') === null)
    return
  const ctx = document.getElementById('treeMapChart').getContext('2d');
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
          // Generate colors based on the category
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

function initializeUsersChart(authorizedUsers , adminUsers , unauthorizedUsers){
  if (document.getElementById('authorizationChart') === null)
    return

  // Calculate total for percentages
  const total = authorizedUsers + adminUsers + unauthorizedUsers;

  // Get the canvas element
  const ctx = document.getElementById('authorizationChart').getContext('2d');

  // Create the pie chart
  new Chart(ctx, {
    type: 'pie',
    data: {
      labels: ['משתמשים מורשים', 'משתמשי מנהל', 'משתמשים לא מורשים'],
      datasets: [{
        data: [authorizedUsers, adminUsers, unauthorizedUsers],
        backgroundColor: [
          '#198754',  // bg-success color for authorized users
          '#0dcaf0',  // bg-info color for admin users
          '#212529'   // bg-dark color for unauthorized users
        ],
        borderWidth: 1
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            font: {
              size: 14
            }
          }
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              const value = context.raw;
              const percentage = ((value / total) * 100).toFixed(1);
              return `${context.label}: ${value} (${percentage}%)`;
            }
          }
        },
        datalabels: {
          color: '#fff',
          font: {
            weight: 'bold',
            size: 18
          },
          formatter: function(value, context) {
            const percentage = ((value / total) * 100).toFixed(1);
            // Only show label if percentage is greater than 0
            if (percentage > 0) {
              return `${value}\n(${percentage}%)`;
            } else {
              return null;  // This will hide the label
            }
          },
          anchor: 'center',
          align: 'right',   // This aligns the label to the right of the anchor point
          offset: 20,       // This moves the label 20 pixels to the right from the anchor point
          padding: {
            left: 0,
            right: 0
          }
        }
      },
      layout: {
        padding: {
          top: 10,
          bottom: 10
        }
      }
    },
    plugins: [ChartDataLabels]
  });
}

// Function to initialize user activity chart
function initUserActivityChart(datesGlobal, countsGlobal) {
  if (document.getElementById('usersActivityChart') === null)
    return

  const ctx = document.getElementById('usersActivityChart')

  const dates = Object.keys(datesGlobal)
  const counts = Object.values(countsGlobal)

  new Chart(ctx, {
    type: 'line',
    data: {
      labels: dates,
      datasets: [{
        label: 'סה״כ משתמשים',
        data: counts,
        borderColor: '#25D366',
        backgroundColor: 'rgba(37, 211, 102, 0.1)',
        fill: true
      }]
    },
    options: {
      responsive: true,
      scales: {
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
        legend: {
          labels: {
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        },
        tooltip: {
          titleFont: {
            size: 14
          },
          bodyFont: {
            size: 16
          },
          callbacks: {
            label: function(context) {
              const date = context.label
              const totalUsers = context.parsed.y
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

