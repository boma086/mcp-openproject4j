import React, { useState, useContext, useCallback } from 'react';
import { useMakeCopilotActionable, useCopilotContext } from "@copilotkit/react/ui";
import '../css/ProjectDashboard.css';

const ProjectDashboard = () => {
  const [report, setReport] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [projectId, setProjectId] = useState('1');
  const [mcpStatus, setMcpStatus] = useState('disconnected');
  const [availableTools, setAvailableTools] = useState([]);
  
  const mcpEndpoint = process.env.REACT_APP_MCP_ENDPOINT || 'http://localhost:8080/api/mcp';
  const apiKey = process.env.REACT_APP_API_KEY;
  const copilotContext = useCopilotContext();

  // Enhanced MCP tool integration with proper error handling
  useMakeCopilotActionable({
    name: "generateWeeklyReport",
    description: "生成项目周报 | Generate weekly project report from OpenProject data",
    parameters: { 
      type: "object", 
      properties: { 
        projectId: { 
          type: "string",
          description: "项目ID | Project ID to analyze"
        }
      },
      required: ["projectId"]
    },
    implementation: async ({ projectId }) => {
      return await executeMcpTool('generateWeeklyReport', { projectId });
    }
  });

  useMakeCopilotActionable({
    name: "analyzeRisks",
    description: "分析项目风险 | Analyze project risks based on overdue tasks",
    parameters: { 
      type: "object", 
      properties: { 
        projectId: { 
          type: "string",
          description: "项目ID | Project ID for risk analysis"
        }
      },
      required: ["projectId"]
    },
    implementation: async ({ projectId }) => {
      return await executeMcpTool('analyzeRisks', { projectId });
    }
  });

  useMakeCopilotActionable({
    name: "calculateCompletion",
    description: "计算项目完成度 | Calculate project completion percentage",
    parameters: { 
      type: "object", 
      properties: { 
        projectId: { 
          type: "string",
          description: "项目ID | Project ID for completion calculation"
        }
      },
      required: ["projectId"]
    },
    implementation: async ({ projectId }) => {
      return await executeMcpTool('calculateCompletion', { projectId });
    }
  });

  // Generic MCP tool execution with enhanced error handling
  const executeMcpTool = useCallback(async (toolName, parameters) => {
    setLoading(true);
    setError(null);
    
    try {
      // First check MCP server connectivity
      await checkMcpConnection();
      
      const response = await fetch(`${mcpEndpoint}/tools/${toolName}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': apiKey ? `Bearer ${apiKey}` : undefined,
          'X-CopilotKit-Request': 'true'
        },
        body: JSON.stringify({
          arguments: parameters,
          context: {
            projectId: parameters.projectId,
            timestamp: new Date().toISOString(),
            source: 'react-dashboard'
          }
        })
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`MCP工具调用失败 | MCP tool call failed: ${response.status} - ${errorData}`);
      }

      const result = await response.json();
      
      // Handle different response formats
      const reportContent = result.content || result.result || result.text || JSON.stringify(result, null, 2);
      
      setReport(reportContent);
      setMcpStatus('connected');
      
      return reportContent;
    } catch (err) {
      const errorMessage = `调用${toolName}时出错 | Error calling ${toolName}: ${err.message}`;
      setError(errorMessage);
      setMcpStatus('error');
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  }, [mcpEndpoint, apiKey]);

  // Check MCP server connection and available tools
  const checkMcpConnection = useCallback(async () => {
    try {
      const response = await fetch(`${mcpEndpoint}/tools`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': apiKey ? `Bearer ${apiKey}` : undefined
        }
      });

      if (response.ok) {
        const tools = await response.json();
        setAvailableTools(tools.tools || tools);
        setMcpStatus('connected');
        return true;
      } else {
        setMcpStatus('error');
        return false;
      }
    } catch (err) {
      setMcpStatus('disconnected');
      return false;
    }
  }, [mcpEndpoint, apiKey]);

  // Enhanced report generation with proper MCP tool calling
  const handleReportGeneration = async (action, projectId) => {
    if (!projectId || projectId.trim() === '') {
      setError('请输入项目ID | Please enter a project ID');
      return;
    }

    // Map UI actions to MCP tool names
    const toolMap = {
      'weekly': 'generateWeeklyReport',
      'risk': 'analyzeRisks', 
      'completion': 'calculateCompletion'
    };

    const toolName = toolMap[action];
    if (!toolName) {
      setError(`未知的报告类型 | Unknown report type: ${action}`);
      return;
    }

    try {
      await executeMcpTool(toolName, { projectId });
    } catch (err) {
      // Error is already handled in executeMcpTool
      console.error('Report generation failed:', err);
    }
  };

  // Clear results and reset state
  const clearReport = () => {
    setReport('');
    setError(null);
  };

  // Get display name for actions with enhanced descriptions
  const getActionDisplayName = (action) => {
    switch (action) {
      case 'weekly':
        return '生成周报 | Generate Weekly';
      case 'risk':
        return '风险分析 | Risk Analysis';
      case 'completion':
        return '完成度 | Completion';
      default:
        return '未知报告 | Unknown Report';
    }
  };

  // Get MCP status indicator
  const getMcpStatusIndicator = () => {
    const statusConfig = {
      'connected': { color: '#27ae60', text: '已连接 | Connected' },
      'disconnected': { color: '#e74c3c', text: '未连接 | Disconnected' },
      'error': { color: '#f39c12', text: '错误 | Error' },
      'connecting': { color: '#3498db', text: '连接中 | Connecting' }
    };
    
    const config = statusConfig[mcpStatus] || statusConfig['disconnected'];
    
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        fontSize: '12px',
        color: config.color,
        fontWeight: 'bold'
      }}>
        <div style={{
          width: '8px',
          height: '8px',
          borderRadius: '50%',
          backgroundColor: config.color,
          animation: mcpStatus === 'connecting' ? 'pulse 1.5s infinite' : 'none'
        }}></div>
        {config.text}
      </div>
    );
  };

  // Check connection on component mount
  React.useEffect(() => {
    checkMcpConnection();
  }, [checkMcpConnection]);

  return (
    <div className="project-dashboard" style={{
      maxWidth: '1200px',
      margin: '0 auto',
      padding: '20px',
      fontFamily: 'Arial, sans-serif',
      backgroundColor: '#f8f9fa',
      borderRadius: '8px',
      boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
    }}>
      <div style={{
        textAlign: 'center',
        marginBottom: '30px',
        padding: '20px',
        backgroundColor: '#fff',
        borderRadius: '8px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <h1 style={{
          color: '#2c3e50',
          margin: '0 0 10px 0',
          fontSize: '2rem',
          fontWeight: 'bold'
        }}>
          项目报告生成器 | Project Report Generator
        </h1>
        <p style={{
          color: '#7f8c8d',
          margin: '0',
          fontSize: '1rem'
        }}>
          AI驱动的OpenProject分析工具 | AI-powered OpenProject Analysis Tool
        </p>
        <div style={{ marginTop: '15px' }}>
          {getMcpStatusIndicator()}
        </div>
      </div>

      <div style={{
        backgroundColor: '#fff',
        padding: '25px',
        borderRadius: '8px',
        marginBottom: '20px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <div style={{ marginBottom: '20px' }}>
          <label htmlFor="projectId" style={{
            display: 'block',
            marginBottom: '8px',
            fontWeight: 'bold',
            color: '#2c3e50'
          }}>
            项目ID | Project ID:
          </label>
          <input
            id="projectId"
            type="text"
            value={projectId}
            onChange={(e) => setProjectId(e.target.value)}
            placeholder="输入项目ID | Enter project ID"
            style={{
              width: '100%',
              padding: '12px',
              border: '2px solid #ddd',
              borderRadius: '6px',
              fontSize: '16px',
              boxSizing: 'border-box',
              transition: 'border-color 0.3s'
            }}
            onFocus={(e) => e.target.style.borderColor = '#3498db'}
            onBlur={(e) => e.target.style.borderColor = '#ddd'}
          />
        </div>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '15px',
          marginBottom: '20px'
        }}>
          {['weekly', 'risk', 'completion'].map((action) => (
            <button
              key={action}
              onClick={() => handleReportGeneration(action, projectId)}
              disabled={loading || mcpStatus !== 'connected'}
              title={mcpStatus !== 'connected' ? 'MCP服务器未连接 | MCP server not connected' : ''}
              style={{
                padding: '15px 20px',
                backgroundColor: loading || mcpStatus !== 'connected' ? '#95a5a6' : 
                  action === 'weekly' ? '#3498db' : 
                  action === 'risk' ? '#e74c3c' : '#27ae60',
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                cursor: loading || mcpStatus !== 'connected' ? 'not-allowed' : 'pointer',
                fontSize: '14px',
                fontWeight: 'bold',
                transition: 'all 0.3s',
                boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                height: '100%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                textAlign: 'center',
                position: 'relative',
                overflow: 'hidden'
              }}
              onMouseOver={(e) => {
                if (!loading && mcpStatus === 'connected') {
                  e.target.style.backgroundColor = 
                    action === 'weekly' ? '#2980b9' : 
                    action === 'risk' ? '#c0392b' : '#229954';
                  e.target.style.transform = 'translateY(-2px)';
                  e.target.style.boxShadow = '0 4px 8px rgba(0,0,0,0.2)';
                }
              }}
              onMouseOut={(e) => {
                if (!loading && mcpStatus === 'connected') {
                  e.target.style.backgroundColor = 
                    action === 'weekly' ? '#3498db' : 
                    action === 'risk' ? '#e74c3c' : '#27ae60';
                  e.target.style.transform = 'translateY(0)';
                  e.target.style.boxShadow = '0 2px 4px rgba(0,0,0,0.1)';
                }
              }}
            >
              <div style={{
                position: 'absolute',
                top: '0',
                left: '0',
                right: '0',
                bottom: '0',
                background: mcpStatus !== 'connected' ? 'rgba(0,0,0,0.1)' : 'none',
                display: mcpStatus !== 'connected' ? 'flex' : 'none',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '20px'
              }}>
                ⚠️
              </div>
              {loading ? (
                <div style={{ display: 'flex', alignItems: 'center' }}>
                  <div style={{
                    width: '16px',
                    height: '16px',
                    border: '2px solid #ffffff',
                    borderTop: '2px solid transparent',
                    borderRadius: '50%',
                    animation: 'spin 1s linear infinite',
                    marginRight: '8px'
                  }}></div>
                  {getActionDisplayName(action).split(' | ')[0]}
                </div>
              ) : getActionDisplayName(action)}
            </button>
          ))}
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button
            onClick={clearReport}
            disabled={loading || (!report && !error)}
            style={{
              padding: '10px 20px',
              backgroundColor: loading || (!report && !error) ? '#95a5a6' : '#f39c12',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: loading || (!report && !error) ? 'not-allowed' : 'pointer',
              fontSize: '14px',
              fontWeight: 'bold',
              transition: 'all 0.3s'
            }}
            onMouseOver={(e) => {
              if (!loading && (report || error)) {
                e.target.style.backgroundColor = '#e67e22';
              }
            }}
            onMouseOut={(e) => {
              if (!loading && (report || error)) {
                e.target.style.backgroundColor = '#f39c12';
              }
            }}
          >
            清除结果 | Clear Results
          </button>
          
          <button
            onClick={checkMcpConnection}
            disabled={loading}
            style={{
              padding: '10px 20px',
              backgroundColor: loading ? '#95a5a6' : '#3498db',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: loading ? 'not-allowed' : 'pointer',
              fontSize: '14px',
              fontWeight: 'bold',
              transition: 'all 0.3s'
            }}
            onMouseOver={(e) => {
              if (!loading) {
                e.target.style.backgroundColor = '#2980b9';
              }
            }}
            onMouseOut={(e) => {
              if (!loading) {
                e.target.style.backgroundColor = '#3498db';
              }
            }}
          >
            检查连接 | Check Connection
          </button>
        </div>
      </div>

      {error && (
        <div style={{
          backgroundColor: '#ffebee',
          color: '#c62828',
          padding: '15px',
          borderRadius: '6px',
          marginBottom: '20px',
          border: '1px solid '#ffcdd2',
          fontWeight: 'bold'
        }}>
          <strong>错误 | Error:</strong> {error}
        </div>
      )}

      {report && (
        <div style={{
          backgroundColor: '#fff',
          padding: '25px',
          borderRadius: '8px',
          marginBottom: '20px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
          border: '1px solid #e0e0e0'
        }}>
          <h3 style={{
            color: '#2c3e50',
            marginTop: '0',
            marginBottom: '20px',
            fontSize: '1.4rem',
            fontWeight: 'bold',
            borderBottom: '2px solid #3498db',
            paddingBottom: '10px'
          }}>
            报告结果 | Report Results
          </h3>
          <div style={{
            backgroundColor: '#f8f9fa',
            padding: '20px',
            borderRadius: '6px',
            border: '1px solid #dee2e6',
            fontFamily: 'monospace',
            fontSize: '14px',
            lineHeight: '1.6',
            whiteSpace: 'pre-wrap',
            wordWrap: 'break-word'
          }}>
            {report}
          </div>
        </div>
      )}

      {loading && (
        <div style={{
          textAlign: 'center',
          padding: '20px',
          backgroundColor: '#fff',
          borderRadius: '8px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        }}>
          <div style={{
            display: 'inline-block',
            width: '40px',
            height: '40px',
            border: '4px solid #f3f3f3',
            borderTop: '4px solid #3498db',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
            marginBottom: '10px'
          }}></div>
          <p style={{
            color: '#2c3e50',
            margin: '10px 0 0 0',
            fontSize: '16px',
            fontWeight: 'bold'
          }}>
            正在生成报告... | Generating report...
          </p>
          <p style={{
            color: '#7f8c8d',
            margin: '5px 0 0 0',
            fontSize: '14px'
          }}>
            请稍候，这可能需要几秒钟时间 | Please wait, this may take a few seconds
          </p>
        </div>
      )}

      <style jsx>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        
        @keyframes pulse {
          0% { opacity: 1; }
          50% { opacity: 0.5; }
          100% { opacity: 1; }
        }
        
        .project-dashboard {
          animation: fadeIn 0.5s ease-in;
        }
        
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(20px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
};

export default ProjectDashboard;